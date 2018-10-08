package com.example.demo.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.code.kaptcha.Producer;
import com.shell.bee.base.exception.UnCheckMsgException;
import com.shell.bee.base.utils.SpringContextUtil;
import com.shell.bee.base.utils.StringUtil;
import com.shell.bee.entity.mvc.RetMsg;
import com.shell.bee.entity.mvc.RetMsg.MsgType;
import com.shell.feign.AuthFeign;
import com.shell.guard.constants.GuardConstant;
import com.shell.guard.security.CustomAuthorizationTokenServices;
import com.shell.guard.service.LoginInfoService;
import com.shell.guard.utils.HttpUtil;
import com.shell.guard.vo.AuthVo;

@DependsOn("authorizationServerTokenServices")
@RestController
public class AuthController {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
	
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    
    @Autowired
    private AuthFeign authFeign;
    
    @Autowired
    private Producer captchaProducer;
    
	RestTemplate restTemplate = new RestTemplate();
	
	@Autowired
	private StringRedisTemplate redisTemplate;
	
	@Autowired
	private LoginInfoService loginInfoService;
	
	@Autowired
	private CustomAuthorizationTokenServices authorizationServerTokenServices;
	
	private AccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
	
	@RequestMapping(value = "/login", method = RequestMethod.POST)
    public RetMsg login(AuthVo authVo, HttpServletRequest request,HttpServletResponse response) throws IOException, ServletException {
	    Map<String,Object> map = new HashMap<String,Object>();
        RetMsg ret = new RetMsg();
        if (!checkAuthVo(authVo, ret)) {
        	return ret;
        }
		if(!authVo.isMsgLogin() && !checkCode(request, authVo.getKaptchaReceived(),ret)) {
			return ret;
		}
		
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
        formData.add("username", authVo.getUsername());
        formData.add("password", authVo.getPassword());
        formData.add("client", authVo.getClient());
        formData.add("grant_type", authVo.getGrant_type());
        formData.add("msgVerifyCode", authVo.getMsgVerifyCode());
        formData.add("isMsgLogin", authVo.isMsgLogin() + "");
        formData.add("entId", authVo.getEntId());
		
        String client = authVo.getClient();
		String clientSecret = loginInfoService.queryClientSecret(client);
        ServiceInstance serviceInstance = loadBalancerClient.choose("guard");
        if (serviceInstance == null) {
            throw new RuntimeException("Failed to choose an auth instance.");
        }
        map = postForMap(serviceInstance.getUri().toString() + SpringContextUtil.getProperty("server.context-path", "") + "/oauth/token", formData, setHeader(client,clientSecret));
        
        if (map != null && map.containsKey("type") && ((String)map.get("type")).equals(MsgType.ERROR)) {
        	throw new UnCheckMsgException((String)map.get("msg"));
        }
        
        String entId = authVo.getEntId();
        if (StringUtil.empty(entId)) {
        	entId = (String)map.get(GuardConstant.TOKEN_SEG_ENT);
        }
        
        loginSuccess(authVo.getUsername(), authVo.getAppCode(), entId, map, response, ret);//登录成功后处理
        logger.info("=============" + authVo.getUsername() + ",登录成功！ ===================");
        return ret;
    }
	
	private void loginSuccess(String userName,String appCode,String entId, Map<String,Object> resultMap,HttpServletResponse response,RetMsg ret) {
		
		if (StringUtil.empty(entId)) {
			entId = "";
		}
		
		//更新缓存,这里做了很多东西，要注意！
        authFeign.afterLogged(userName, entId , appCode);
        
        String access_token = (String)resultMap.get("access_token");
        String token = "Bearer" + access_token;
        response.setHeader("Authorization", token);//设置头部信息
        
        //封装返回对象
        resultMap.put("sso",true);
        ret.setType(MsgType.SUCCESS);
        ret.setShow(true);
        ret.setMsg("登录成功！");
        ret.setBean(resultMap);
        
        //设置cookie
        Cookie tokenCookie = new Cookie("access_token",token);
        tokenCookie.setMaxAge(-1);//存在内存中，关闭浏览器失效
        tokenCookie.setPath("/");
        response.addCookie(tokenCookie);
        
        //设置过期时间
        String key = access_token + GuardConstant.TOKEN_KEY;
        redisTemplate.opsForValue().set(key, GuardConstant.TOKEN_VALID, GuardConstant.TOKEN_EXPIRE_IN, TimeUnit.SECONDS);
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean checkAuthVo(AuthVo authVo, RetMsg ret) {
		if (StringUtil.emptyAndNull(authVo.getUsername())) {
			ret.setType(MsgType.WARING);
            ret.setMsg("用户不能为空！");
            return false;
		}
		
		if (!authVo.isMsgLogin() && StringUtil.emptyAndNull(authVo.getPassword())) {
			ret.setType(MsgType.WARING);
            ret.setMsg("密码不能为空！");
            return false;
		}
		
		if (authVo.isMsgLogin() && StringUtil.emptyAndNull(authVo.getMsgVerifyCode())) {
			ret.setType(MsgType.WARING);
            ret.setMsg("短信验证码！");
            return false;
		}
		
		return true;
	}
	
	/**
	 * 检查验证码
	 * @param request
	 * @param kaptchaReceived
	 * @param ret
	 */
	private boolean checkCode(HttpServletRequest request,String kaptchaReceived,RetMsg ret) {
		String code = HttpUtil.getCookieValue(request, com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY);
		boolean checkSuccess = true;
        if (StringUtil.emptyAndNull(kaptchaReceived)) {
        	checkSuccess = false;
            ret.setType(MsgType.WARING);
            ret.setMsg("验证码不能为空！");
        } else if(code == null) {
        	checkSuccess = false;
        	ret.setType(MsgType.WARING);
            ret.setMsg("验证码失效！");
        } else if(!code.equals(kaptchaReceived)) {
        	checkSuccess = false;
        	ret.setType(MsgType.ERROR);
            ret.setMsg("验证码输入错误！");
        }
        return checkSuccess;
	}
	
	@RequestMapping("/auth/getVerifycode")
	public void getVerifyCode(HttpServletRequest request, HttpServletResponse response) throws Exception{
		try {
			logger.debug("------------begin get verify code pic ----------");
			// Set to expire far in the past.
			response.setDateHeader("Expires", 0);
			// Set standard HTTP/1.1 no-cache headers.
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
			// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
			response.addHeader("Cache-Control", "post-check=0, pre-check=0");
			// Set standard HTTP/1.0 no-cache header.
			response.setHeader("Pragma", "no-cache");
			// return a jpeg
			response.setContentType("image/jpeg");
			// create the text for the image
			String capText = captchaProducer.createText();
			// store the text in the session
//			SecurityUtils.getSubject().getSession().setAttribute(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY, capText);
			Cookie imageCode = new Cookie(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY,capText);
			//保存到redis
			
			imageCode.setPath("/");
			response.addCookie(imageCode);
			// create the image with the text
			BufferedImage bi = captchaProducer.createImage(capText);
			ServletOutputStream out = response.getOutputStream();
			// write the data out
			ImageIO.write(bi, "jpg", out);
			try {
				out.flush();
			} finally {
				out.close();
			}
			logger.debug("------------end get verify code pic ----------");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/auth/change-ent", method = RequestMethod.POST)
    public RetMsg changeEnt(AuthVo authVo,HttpServletRequest request,HttpServletResponse response) throws IOException, ServletException {
	    
		 RetMsg ret = new RetMsg();
		//判断cookies里是否包含token
		Cookie[] cookies =  request.getCookies();
		String token = null;
		if(null != cookies){
			for(Cookie cookie : cookies) {
				if(cookie.getName().equals("access_token")) {
					token = (String)cookie.getValue();
					break;
				}
			}
		}
		
		
		token = token.substring(6);
        OAuth2AccessToken realToken = authorizationServerTokenServices.readAccessToken(token);
  		OAuth2Authentication authentication = authorizationServerTokenServices.loadAuthentication(realToken.getValue());
  	    UserDetails userDetails = (UserDetails)authentication.getPrincipal();

  	    MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
  	    if (StringUtil.emptyAndNull(userDetails.getPassword())) {
  	    	ret.setMsg("当前不是使用密码登录，请重新登录再切换企业！");
  	    	ret.setType(MsgType.WARING);
  	    	return ret;
  	    }
 
  	    authVo.setUsername(userDetails.getUsername());
  	    authVo.setPassword(userDetails.getPassword());
  	    formData.add("entId", authVo.getEntId());
  	    formData.add("username", authVo.getUsername());
  	    formData.add("password", authVo.getPassword());
  	    formData.add("client", authVo.getClient());
  	    formData.add("grant_type", authVo.getGrant_type());
  	  
		
        String client = authVo.getClient();
		String clientSecret = loginInfoService.queryClientSecret(client);
        ServiceInstance serviceInstance = loadBalancerClient.choose(SpringContextUtil.getProperty("spring.application.name", "guard"));
        if (serviceInstance == null) {
            throw new RuntimeException("Failed to choose an auth instance.");
        }
        
        Map<String,Object> map = new HashMap<String,Object>();
        map = postForMap(serviceInstance.getUri().toString() + SpringContextUtil.getProperty("server.context-path", "") + "/oauth/token", formData, setHeader(client,clientSecret));
        
        if (map != null && map.containsKey("type") && ((String)map.get("type")).equals(MsgType.ERROR)) {
        	throw new UnCheckMsgException((String)map.get("msg"));
        }
        
        loginSuccess(authVo.getUsername(), authVo.getAppCode(), authVo.getEntId(), map, response, ret);//登录成功后处理
        ret.setMsg("企业切换成功！");
        return ret;
    }
	
	/**
	 * token验证，包含判断token是否即将过期，如果是将创建新的token
	 * @param token
	 * @return
	 * @throws ServletException 
	 * @throws IOException 
	 */
	@RequestMapping(value = "/auth/check_token", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String,Object>> checkToken(@RequestParam("token") String token) throws IOException, ServletException {
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Cache-Control", "no-store");
		headers.set("Pragma", "no-cache");
		Map<String,Object> result = new HashMap<String,Object>();
		ResponseEntity<Map<String,Object>> responseEntity = new ResponseEntity<Map<String,Object>>(result, headers, HttpStatus.OK);

		//封装头部，forward请求check_token
		if (isJwtBearerToken(token)) {
            token = token.substring(6);
            OAuth2AccessToken realToken = authorizationServerTokenServices.readAccessToken(token);
    		if (realToken == null) {
    			logger.error("Token was not recognised");
    			result.put("success", false);
    			return responseEntity;
    		}

    		OAuth2Authentication authentication = authorizationServerTokenServices.loadAuthentication(realToken.getValue());

    		Map<String, ?> response = accessTokenConverter.convertAccessToken(realToken, authentication);
    		
    		result.put("response", response);
    		//使用redis进行登录过期控制
    		String key = token + GuardConstant.TOKEN_KEY;
    		String valid = redisTemplate.opsForValue().get(key);
    		if(valid == null) {
    			logger.error("Token has expired");
    			result.put("success", false);
    			return responseEntity;
    		} else {
    	        redisTemplate.opsForValue().set(key, GuardConstant.TOKEN_VALID, GuardConstant.TOKEN_EXPIRE_IN, TimeUnit.SECONDS);//续期
    		}
    		
		}
		result.put("success", true);
		return responseEntity;
	}
	
    private boolean isJwtBearerToken(String token) {
        return StringUtils.countMatches(token, ".") == 2 && (token.startsWith("Bearer") || token.startsWith("bearer"));
    }
	
    private Map<String, Object> postForMap(String path, MultiValueMap<String, String> formData, HttpHeaders headers) {
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }
        @SuppressWarnings("rawtypes")
        Map map = restTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class).getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = map;
        return result;
    }
    
	public HttpHeaders setHeader(String clientId,String clientSecret)
			throws IOException, ServletException {
		HttpHeaders headers = new HttpHeaders();
		String authHead = getAuthorizationHeader(clientId, clientSecret);
		headers.set("Authorization", authHead);
		headers.set("X-Requested-With", "XMLHttpRequest");
		return headers;
	}
	
    private String getAuthorizationHeader(String clientId, String clientSecret) {
        String creds = String.format("%s:%s", clientId, clientSecret);
        try {
            return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not convert String");
        }
    }
}

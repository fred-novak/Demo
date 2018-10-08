package com.example.demo.model;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.shell.bee.base.exception.UnCheckMsgException;
import com.shell.bee.base.utils.Encryptor;
import com.shell.bee.base.utils.StringUtil;
import com.shell.bee.cache.Cache;
import com.shell.bee.cache.CacheBuilder;
import com.shell.bee.entity.auth.AuthUser;
import com.shell.feign.AuthFeign;
import com.shell.feign.PubOrganFeign;
import com.shell.governor.organization.vo.PubOrganVo;
import com.shell.guard.exception.ErrorCodes;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private AuthFeign authFeign; //到时替换为我们的jar包。
    @Autowired
    private PubOrganFeign organFeign;
    
    /**
	 * 定义缓存块
	 */
	private static Cache<String, String> CACHE;

	//这里是登录post请求的方法
    @SuppressWarnings("all")
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password;
        Map data;
        if(authentication.getDetails() instanceof Map) {
            data = (Map) authentication.getDetails();
        }else{
            return null;
        }
        String clientId = (String) data.get("client");
        Assert.hasText(clientId, "clientId must have value");
        String type = (String) data.get("type");
        String entId = (String) data.get("entId");
        String msgVerifyCode = (String) data.get("msgVerifyCode");
        String isMsgLogin = (String) data.get("isMsgLogin");

        password = (String) authentication.getCredentials();
        AuthUser authUser = null;
        authUser = authFeign.getUserInfoByUserName(username);
        if ("true".equals(isMsgLogin)) {
        	checkPhoneAndMsgVerifyCode(username, msgVerifyCode);
        } else {
        	checkUsernameAndPassword(authUser,username, password);
        }
        if (StringUtil.empty(entId)) {
        	if (authUser.getEnt() != null && StringUtil.notEmpty(authUser.getEnt().getOrganId())) {
        		entId = authUser.getEnt().getOrganId();
        	} else {
        		List<PubOrganVo> organs = organFeign.queryUserEnt(authUser.getUserId());
                if (organs != null && organs.size() > 0) {
                	entId = organs.get(0).getOrganId();
                }
        	}
        }
        CustomUserDetails customUserDetails = buildCustomUserDetails(username, password, authUser.getUserId(), clientId, entId);
        return new CustomAuthenticationToken(customUserDetails);
    }

    private CustomUserDetails buildCustomUserDetails(String username, String password, String userId, String clientId,String ent) {
        CustomUserDetails customUserDetails = new CustomUserDetails.CustomUserDetailsBuilder()
                .withUserId(userId)
                .withPassword(password)
                .withUsername(username)
                .withClientId(clientId)
                .withEnt(ent)
                .build();
        return customUserDetails;
    }

    //检测用户名和密码是否合法
    private void checkUsernameAndPassword(AuthUser authUser,String username, String password) {
    	String passwordMd5 = Encryptor.encryptWithMD5(password);//MD5加密
        if(authUser != null) {
        	String realPassword = authUser.getPassword();
        	if(!passwordMd5.equals(realPassword)) {
        		throw new UnCheckMsgException(ErrorCodes.INVALID_USERNAME_OR_PASSWORD.getDetailMessage());
        	}
        } else {
        	throw new UnCheckMsgException(ErrorCodes.INVALID_USERNAME_OR_PASSWORD.getDetailMessage());
        }
    }
    
    //检查短信验证码是否正常
    private void checkPhoneAndMsgVerifyCode(String phone, String msgVerifyCode) {
    	if(CACHE == null) {
			CACHE = CacheBuilder.newBuilder().build();
		}
    	
    	if (CACHE.exists(phone)) {
    		String smsCode = CACHE.get(phone);
    		if (!smsCode.equals(msgVerifyCode)) {
    			throw new UnCheckMsgException(ErrorCodes.INVALID_MSG_VERIFY_CODE.getDetailMessage());
    		}
    	} else {
    		throw new UnCheckMsgException(ErrorCodes.INVALID_MSG_VERIFY_CODE.getDetailMessage());
    	}
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }

}
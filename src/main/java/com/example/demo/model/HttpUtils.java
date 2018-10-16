package com.cloudlinkin.activity.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cloudlinkin.activity.constants.Global;
import com.cloudlinkin.activity.exception.ActivityException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * @ClassName: HttpUtils
 * @Description: Http请求工具类
 * @author Jason 2017年2月27日 下午9:36:05
 *
 */
public class HttpUtils {

	private static final Logger	logger			= LoggerFactory.getLogger(HttpUtils.class);

	private RequestConfig		requestConfig	= RequestConfig.custom().setSocketTimeout(50000)
			.setConnectTimeout(50000).setConnectionRequestTimeout(50000).build();

	private static HttpUtils	instance		= null;

	private HttpUtils() {
	}

	public static HttpUtils getInstance() {
		if (instance == null) {
			instance = new HttpUtils();
		}
		return instance;
	}

	/**
	 * 
	 * @Title: sendHttpPost
	 * @Description: 发送 post请求
	 * @author Jason 2017年2月27日 下午10:08:50
	 *
	 * @param httpUrl
	 *            地址
	 * @return
	 */
	public String sendHttpPost(String httpUrl) {
		HttpPost httpPost = new HttpPost(httpUrl);// 创建httpPost
		return sendHttpPost(httpPost);
	}

	/**
	 * 
	 * @Title: sendHttpPost
	 * @Description: 发送 post请求
	 * @author Jason 2017年2月27日 下午10:08:30
	 *
	 * @param httpUrl
	 *            地址
	 * @param params
	 *            参数(格式:key1=value1&key2=value2)
	 * @return
	 */
	public String sendHttpPost(String httpUrl, String params) {
		HttpPost httpPost = new HttpPost(httpUrl);// 创建httpPost
		try {
			// 设置参数
			StringEntity stringEntity = new StringEntity(params, "UTF-8");
			stringEntity.setContentType("application/x-www-form-urlencoded");
			httpPost.setEntity(stringEntity);
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return sendHttpPost(httpPost);
	}

	/**
	 * 
	 * @Title: HttpClientPost
	 * @Description: 通过httpClient进行post提交
	 * @author Jason 2017年2月27日 下午9:52:32
	 *
	 * @param url
	 * @param charset
	 * @param maps
	 * @return
	 * @throws Exception
	 */
	public String HttpClientPost(String url, String charset, LinkedHashMap<String, String> maps) {
		HttpPost httpPost = new HttpPost(url);// 创建httpPost
		// NameValuePair数组对象用于传入参数
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
		// 创建参数队列
		List<NameValuePair> nameValuePairs = Lists.newArrayList();
		for (String key : maps.keySet()) {
			nameValuePairs.add(new BasicNameValuePair(key, maps.get(key)));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return sendHttpPost(httpPost);
	}

	/**
	 * 
	 * @Title: HttpClientPost
	 * @Description: 通过httpClient进行post提交，返回对象类型
	 * @author Jason 2017年2月27日 下午10:05:19
	 *
	 * @param url
	 * @param charset
	 * @param maps
	 * @param clazz
	 * @return
	 */
	public <T> T HttpClientPost(String url, String charset, JSONObject jsonObject, Class<T> clazz) {
		return JSON.parseObject(sendHttpsPostByJson(url, charset, jsonObject), clazz);
	}
	
	
	public <T> T HttpClientPostWithouts(String url, String charset, JSONObject jsonObject, Class<T> clazz) {
		return JSON.parseObject(sendHttpPostByJson(url, charset, jsonObject), clazz);
	}
	
	public JSONObject HttpClientPostXml(String url, String charset, String xmlObject) {
		return   JSON.parseObject(sendHttpPostByXml(url, charset, xmlObject));
				//JSON.parseObject(sendHttpPostByJson(url, charset, jsonObject), clazz);
				
	}
	/**
	 * 新建json请求
	 * @author xiaofd
	 * @param url
	 * @param charset
	 * @param jsonObject
	 * @return
	 */
	public JSONObject HttpClientPostJson(String url, String charset, JSONObject jsonObject) {
		return   JSON.parseObject(sendHttpPostByJson(url, charset, jsonObject));
				//JSON.parseObject(sendHttpPostByJson(url, charset, jsonObject), clazz);
	}
	
	
	public String sendHttpsPostByJson(String url, String charset, JSONObject json) {
		HttpPost httpPost = new HttpPost(url);// 创建httpPost
		// NameValuePair数组对象用于传入参数
		httpPost.setHeader("Content-Type", "application/json");
		// 创建参数队列
		/*List<NameValuePair> nameValuePairs = Lists.newArrayList();
		for (String key : maps.keySet()) {
			nameValuePairs.add(new BasicNameValuePair(key, maps.get(key)));
		}*/
		try {
		//	httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
			httpPost.setEntity(new StringEntity(json.toString(), "utf-8"));
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return sendHttpsPost(httpPost);
	}
	
	public String sendHttpPostByJson(String url, String charset, JSONObject json) {
		HttpPost httpPost = new HttpPost(url);// 创建httpPost
		httpPost.setHeader("Content-Type", "application/json");
		try {
			httpPost.setEntity(new StringEntity(json.toString(), "utf-8"));
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return sendHttpPost(httpPost);
	}
	
	public String sendHttpPostByXml(String url, String charset, String xmlObject) {
		HttpPost httpPost = new HttpPost(url);// 创建httpPost
		httpPost.setHeader("Content-Type", "text/plain");
		try {
			httpPost.setEntity(new StringEntity(xmlObject, "utf-8"));
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return sendHttpPost(httpPost);
	}
	

	/**
	 * 
	 * @Title: sendHttpPost
	 * @Description: 发送 post请求（带文件）
	 * @author Jason 2017年2月27日 下午10:08:16
	 *
	 * @param httpUrl
	 * @param maps
	 * @param fileLists
	 * @return
	 */
	public String sendHttpPost(String httpUrl, LinkedHashMap<String, String> maps, List<File> fileLists) {
		HttpPost httpPost = new HttpPost(httpUrl);// 创建httpPost
		MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create();
		meBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		meBuilder.setCharset(Charset.forName("UTF-8"));
		for (String key : maps.keySet()) {
			meBuilder.addPart(key, new StringBody(maps.get(key), ContentType.TEXT_PLAIN));
		}
		for (File file : fileLists) {
			FileBody fileBody = new FileBody(file);
			meBuilder.addPart("files", fileBody);
		}
		HttpEntity reqEntity = meBuilder.build();
		httpPost.setEntity(reqEntity);
		return sendHttpPost(httpPost);
	}

	public String sendHttpPostByStream(String httpUrl, LinkedHashMap<String, String> maps,
			List<InputStreamBody> inutPutLists) {
		HttpPost httpPost = new HttpPost(httpUrl);// 创建httpPost
		MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create();
		meBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		meBuilder.setCharset(Charset.forName("UTF-8"));
		for (String key : maps.keySet()) {
			meBuilder.addPart(key, new StringBody(maps.get(key), ContentType.TEXT_PLAIN));
		}
		for (InputStreamBody in : inutPutLists) {
			meBuilder.addPart("files", in);
		}
		HttpEntity reqEntity = meBuilder.build();
		httpPost.setEntity(reqEntity);
		return sendHttpPost(httpPost);
	}

	/**
	 * 
	 * @Title: sendHttpPost
	 * @Description: 发送 post请求（带文件），返回对象类型
	 * @author Jason 2017年2月27日 下午10:07:54
	 *
	 * @param httpUrl
	 * @param maps
	 * @param fileLists
	 * @param clazz
	 * @return
	 */
	public <T> T sendHttpPost(String httpUrl, LinkedHashMap<String, String> maps, List<File> fileLists,
			Class<T> clazz) {
		return JSON.parseObject(sendHttpPost(httpUrl, maps, fileLists), clazz);
	}

	/**
	 * 
	 * @Title: sendHttpsPost
	 * @Description: 发送Https Post请求
	 * @author Jason 2017年2月27日 下午10:14:17
	 *
	 * @param url
	 * @param charset
	 * @param maps
	 * @return
	 */
	public String sendHttpsPost(String url, String charset, LinkedHashMap<String, String> maps) {
		HttpPost httpPost = new HttpPost(url);// 创建httpPost
		// NameValuePair数组对象用于传入参数
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
		// 创建参数队列
		List<NameValuePair> nameValuePairs = Lists.newArrayList();
		for (String key : maps.keySet()) {
			nameValuePairs.add(new BasicNameValuePair(key, maps.get(key)));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return sendHttpsPost(httpPost);
	}

	public Map<String, String> getHttpCookie(String url, String charset, LinkedHashMap<String, String> maps) {
		HttpPost httpPost = new HttpPost(url);// 创建httpPost
		// NameValuePair数组对象用于传入参数
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
		// 创建参数队列
		List<NameValuePair> nameValuePairs = Lists.newArrayList();
		for (String key : maps.keySet()) {
			nameValuePairs.add(new BasicNameValuePair(key, maps.get(key)));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return getHttpCookie(httpPost);
	}

	/**
	 * 
	 * @Title: sendHttpPost
	 * @Description: 发送Post请求
	 * @author Jason 2017年2月27日 下午10:12:12
	 *
	 * @param httpPost
	 * @return
	 */
	private String sendHttpPost(HttpPost httpPost) {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		String responseContent = null;
		try {
			// 创建默认的httpClient实例.
			httpClient = HttpClients.createDefault();
			httpPost.setConfig(requestConfig);
			// 执行请求
			response = httpClient.execute(httpPost);
			entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, "UTF-8");
		} catch (ConnectTimeoutException e) {
			throw new ActivityException("连接超时！", Global.CONFIG_ERROR);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				// 关闭连接,释放资源
				if (response != null) {
					response.close();
				}
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return responseContent;
	}

	/**
	 * 
	 * @Title: sendHttpsPost
	 * @Description: 发送Https Post请求
	 * @author Jason 2017年2月27日 下午10:12:02
	 *
	 * @param httpPost
	 * @return
	 */
	private String sendHttpsPost(HttpPost httpPost) {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		String responseContent = null;
		try {
			// 创建默认的httpClient实例.
			PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader
					.load(new URL(httpPost.getURI().toString()));

			DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);
			httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();
			httpPost.setConfig(requestConfig);
			// 执行请求
			response = httpClient.execute(httpPost);
			entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, "UTF-8");
		} catch (Exception e) {
			logger.error(e.toString());
		} finally {
			try {
				// 关闭连接,释放资源
				if (response != null) {
					response.close();
				}
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				logger.error(e.toString());
			}
		}
		return responseContent;
	}

	private Map<String, String> getHttpCookie(HttpPost httpPost) {
		Map<String, String> cookieMap = Maps.newHashMap();
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		try {
			// 创建默认的httpClient实例.
			PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader
					.load(new URL(httpPost.getURI().toString()));

			DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);
			httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();
			httpPost.setConfig(requestConfig);
			// 执行请求
			response = httpClient.execute(httpPost);
			Header[] cookies = response.getHeaders("Set-Cookie");
			for (int j = 0; j < cookies.length; j++) {
				HeaderElement[] cookie = cookies[j].getElements();
				for (int i = 0; i < cookie.length; i++) {
					cookieMap.put(cookie[i].getName(), cookie[i].getValue());
				}
			}
		} catch (Exception e) {
			logger.error(e.toString());
		} finally {
			try {
				// 关闭连接,释放资源
				if (response != null) {
					response.close();
				}
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				logger.error(e.toString());
			}
		}
		return cookieMap;
	}

	/**
	 * 
	 * @Title: sendHttpGet
	 * @Description: 发送 get请求
	 * @author Jason 2017年2月27日 下午10:10:48
	 *
	 * @param httpUrl
	 *            地址
	 * @return
	 */
	public String sendHttpGet(String httpUrl) {
		HttpGet httpGet = new HttpGet(httpUrl);// 创建get请求
		return sendHttpGet(httpGet);
	}

	/**
	 * 
	 * @Title: sendHttpGet
	 * @Description: 发送 get请求,返回json
	 * @author ld 2017年3月3日 下午2:48:16
	 *
	 * @param httpUrl
	 * @param clazz
	 * @return
	 */
	public <T> T sendHttpGet(String httpUrl, Class<T> clazz) {
		HttpGet httpGet = new HttpGet(httpUrl);// 创建get请求
		return JSON.parseObject(sendHttpGet(httpGet), clazz);
	}

	/**
	 * 
	 * @Title: sendHttpsGet
	 * @Description: 发送 get请求Https
	 * @author Jason 2017年2月27日 下午10:10:08
	 *
	 * @param httpUrl
	 * @return
	 */
	public String sendHttpsGet(String httpUrl) {
		HttpGet httpGet = new HttpGet(httpUrl);// 创建get请求
		return sendHttpsGet(httpGet);
	}

	/**
	 * 
	 * @Title: sendHttpGet
	 * @Description: 发送Get请求
	 * @author Jason 2017年2月27日 下午10:10:16
	 *
	 * @param httpGet
	 * @return
	 */
	public String sendHttpGet(HttpGet httpGet) {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		String responseContent = null;
		try {
			// 创建默认的httpClient实例.
			httpClient = HttpClients.createDefault();
			httpGet.setConfig(requestConfig);
			// 执行请求
			response = httpClient.execute(httpGet);
			entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, "UTF-8");
		} catch (Exception e) {
			logger.error(e.toString());
		} finally {
			try {
				// 关闭连接,释放资源
				if (response != null) {
					response.close();
				}
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				logger.error(e.toString());
			}
		}
		return responseContent;
	}

	/**
	 * 
	 * @Title: sendHttpsGet
	 * @Description: 发送Get请求Https
	 * @author Jason 2017年2月27日 下午10:10:26
	 *
	 * @param httpGet
	 * @return
	 */
	private String sendHttpsGet(HttpGet httpGet) {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		String responseContent = null;
		try {
			// 创建默认的httpClient实例.
			PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader
					.load(new URL(httpGet.getURI().toString()));
			DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);
			httpClient = HttpClients.custom().setSSLHostnameVerifier(hostnameVerifier).build();
			httpGet.setConfig(requestConfig);
			// 执行请求
			response = httpClient.execute(httpGet);
			entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, "UTF-8");
		} catch (Exception e) {
			logger.error(e.toString());
		} finally {
			try {
				// 关闭连接,释放资源
				if (response != null) {
					response.close();
				}
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				logger.error(e.toString());
			}
		}
		return responseContent;
	}

	/**
	 * 
	 * @Title: getParamMap
	 * @Description: 从URL中提取请求参数列表
	 * @author Jason 2017年6月15日 下午2:38:28
	 *
	 * @param url
	 * @return
	 */
	public Map<String, String> getParamMap(String url) {
		Map<String, String> paramMap = Maps.newLinkedHashMap();
		String paramString = url.substring(url.indexOf("?") + 1);
		List<String> paraList = Arrays.asList(paramString.split("&"));
		for(String paramSubString :paraList) {
			if (paramSubString.indexOf("=") > -1) {
				String[] paramKeyVal = paramSubString.split("=");
				if (paramKeyVal.length > 1) {
					paramMap.put(paramKeyVal[0], paramKeyVal[1]);
				} else {
					paramMap.put(paramKeyVal[0], StringUtils.EMPTY);
				}
			}
		};
		return paramMap;
	}

	public static void main(String[] args) {
		HttpUtils httpUtils = HttpUtils.getInstance();
		System.err.println(httpUtils.sendHttpsGet("https://www.baidu.com"));
	}
}

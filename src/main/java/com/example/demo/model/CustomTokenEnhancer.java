package com.example.demo.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpRequest;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import com.shell.guard.constants.GuardConstant;


public class CustomTokenEnhancer extends JwtAccessTokenConverter implements Serializable {

	private static final long serialVersionUID = -4802799816735213420L;

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken,
                                     OAuth2Authentication authentication) {
    	HttpRequest req;
    	
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        authentication.getUserAuthentication().getPrincipal();
        Map<String, Object> info = new HashMap<>();
        info.put(GuardConstant.TOKEN_SEG_USER_ID, userDetails.getUserId());
        info.put(GuardConstant.TOKEN_SEG_ENT, userDetails.getEnt());

        DefaultOAuth2AccessToken customAccessToken = new DefaultOAuth2AccessToken(accessToken);
        customAccessToken.setAdditionalInformation(info);

        OAuth2AccessToken enhancedToken = super.enhance(customAccessToken, authentication);
        enhancedToken.getAdditionalInformation().put(GuardConstant.TOKEN_SEG_CLIENT, userDetails.getClientId());
        return enhancedToken;
    }

}

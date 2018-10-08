package com.example.demo.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;

import com.shell.guard.security.CustomAuthenticationProvider;

@Configuration
public class AuthenticationManagerConfig extends GlobalAuthenticationConfigurerAdapter {

    @Autowired
    CustomAuthenticationProvider customAuthenticationProvider;
    //AuthenticationManagerBuilder是用来创建AuthenticationManager，允许自定义提供多种方式的AuthenticationProvider，比如LDAP、基于JDBC等等
    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
    	//使用自定义的AuthenticationProvider
        auth.authenticationProvider(customAuthenticationProvider);
    }

}

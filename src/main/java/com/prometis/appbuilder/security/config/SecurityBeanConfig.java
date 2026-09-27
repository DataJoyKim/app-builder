package com.prometis.appbuilder.security.config;

import com.prometis.appbuilder.security.token.JwtProvider;
import com.prometis.core.crypto.BCryptPasswordEncoder;
import com.prometis.core.crypto.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityBeanConfig {
    @Autowired
    private SecurityProperties config;

    @Bean
    public JwtProvider jwtProvider() {
        return new JwtProvider(config.getTokenJwtSecretKey(), config.getTokenAccessTokenExpireTime(), config.getTokenRefreshTokenExpireTime());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

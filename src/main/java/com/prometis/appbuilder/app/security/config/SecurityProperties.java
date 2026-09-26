package com.prometis.appbuilder.app.security.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SecurityProperties {
    private String tokenJwtSecretKey = "mySuperSecureJwtSecretKeyThatIsAtLeast32Bytes";
    private Long tokenAccessTokenExpireTime = 1000L * 60 * 15;
    private Long tokenRefreshTokenExpireTime = 1000L * 60 * 60 * 24 * 7;
    private String loginPath = "/login";
    private String consoleAccessPermitAuthorityCode = "SYS_ADMIN";
}

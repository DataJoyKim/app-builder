package com.prometis.appbuilder.platform.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 서버 기동 시 만드는 플랫폼 관리자(sysadmin) 계정 설정 (application.yml 의 platform.sysadmin).
 * 계정이 이미 있으면 아무것도 바꾸지 않으므로, 초기 비밀번호는 처음 만들 때만 쓰인다.
 */
@ConfigurationProperties(prefix = "platform.sysadmin")
public record SysAdminProperties(
        String loginId,
        String initialPassword,
        String userName,
        String email
) {
    public SysAdminProperties {
        loginId = isBlank(loginId) ? "sysadmin" : loginId.trim();
        userName = isBlank(userName) ? loginId : userName.trim();
        email = email == null ? "" : email.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

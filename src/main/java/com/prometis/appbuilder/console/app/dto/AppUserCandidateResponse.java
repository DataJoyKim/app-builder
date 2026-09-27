package com.prometis.appbuilder.console.app.dto;

import com.prometis.appbuilder.platform.user.User;
import lombok.Builder;
import lombok.Getter;

/**
 * 애플리케이션 사용자로 등록할 수 있는 플랫폼 사용자. 비밀번호/플랫폼 권한은 내보내지 않는다.
 */
@Getter
@Builder
public class AppUserCandidateResponse {
    private Long userId;
    private String loginId;
    private String userName;
    private String email;

    public static AppUserCandidateResponse of(User user) {
        return AppUserCandidateResponse.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .build();
    }
}

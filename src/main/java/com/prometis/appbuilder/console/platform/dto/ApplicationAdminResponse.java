package com.prometis.appbuilder.console.platform.dto;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.platform.user.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationAdminResponse {
    private Long id;
    private String applicationId;
    private Long userId;
    private String loginId;
    private String userName;
    private String email;
    private String authority;

    // 사용자가 삭제된 경우 user 는 null 이다
    public static ApplicationAdminResponse of(AppUser appUser, User user) {
        return ApplicationAdminResponse.builder()
                .id(appUser.getId())
                .applicationId(appUser.getApplicationId())
                .userId(appUser.getUserId())
                .loginId(user == null ? null : user.getLoginId())
                .userName(user == null ? null : user.getUserName())
                .email(user == null ? null : user.getEmail())
                .authority(appUser.getAuthority())
                .build();
    }
}

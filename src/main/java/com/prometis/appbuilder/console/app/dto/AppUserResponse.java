package com.prometis.appbuilder.console.app.dto;

import com.prometis.appbuilder.app.security.appuser.AppUser;
import com.prometis.appbuilder.platform.user.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppUserResponse {
    private Long id;
    private Long userId;
    private String loginId;
    private String userName;
    private String email;
    private String authority;

    // 사용자가 삭제된 경우 user 는 null 이다
    public static AppUserResponse of(AppUser appUser, User user) {
        return AppUserResponse.builder()
                .id(appUser.getId())
                .userId(appUser.getUserId())
                .loginId(user == null ? null : user.getLoginId())
                .userName(user == null ? null : user.getUserName())
                .email(user == null ? null : user.getEmail())
                .authority(appUser.getAuthority())
                .build();
    }
}

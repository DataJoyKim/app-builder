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
    // 소속 회사. 비어 있으면 회사 미지정
    private String companyCode;
    // 로그인한 본인 행인지. 본인은 권한 변경/삭제할 수 없다
    private boolean me;

    // 사용자가 삭제된 경우 user 는 null 이다. loginUserId 를 모르면 null (본인 행 없음)
    public static AppUserResponse of(AppUser appUser, User user, Long loginUserId) {
        return AppUserResponse.builder()
                .id(appUser.getId())
                .userId(appUser.getUserId())
                .loginId(user == null ? null : user.getLoginId())
                .userName(user == null ? null : user.getUserName())
                .email(user == null ? null : user.getEmail())
                .authority(appUser.getAuthority())
                .companyCode(appUser.getCompanyCode())
                .me(loginUserId != null && loginUserId.equals(appUser.getUserId()))
                .build();
    }
}

package com.prometis.appbuilder.security.domain;

import com.prometis.appbuilder.platform.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter @AllArgsConstructor @Builder
public class AuthenticatedUser {
    private Long userId;
    private String userName;
    private List<GrantedPermission> grantedPermissions;

    public static AuthenticatedUser createAuthenticatedUser(User user) {
        return AuthenticatedUser.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .build();
    }

    public void grantPermission(String code) {
        if(this.grantedPermissions == null) {
            this.grantedPermissions = new ArrayList<>();
        }

        this.grantedPermissions.add(
                GrantedPermission.builder()
                        .role(code)
                        .build()
        );
    }
}

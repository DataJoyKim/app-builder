package com.prometis.appbuilder.app.security.domain;

import com.prometis.appbuilder.app.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter @AllArgsConstructor @Builder
public class AuthenticatedUser {
    private Long userId;
    private String userName;
    private List<GrantedAuthority> grantedAuthorities;

    public static AuthenticatedUser createAuthenticatedUser(User user) {
        return AuthenticatedUser.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .build();
    }

    public void grantAuthority(Authority authority) {
        if(authority == null) {
            return;
        }

        if(this.grantedAuthorities == null) {
            this.grantedAuthorities = new ArrayList<>();
        }

        this.grantedAuthorities.add(
                GrantedAuthority.builder()
                        .role(authority.getCode())
                        .build()
        );
    }
}

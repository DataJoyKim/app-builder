package com.prometis.appbuilder.app.security.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @AllArgsConstructor @Builder
public class GrantedAuthority {
    private String role;

    public static boolean hasAuthority(List<GrantedAuthority> grantedAuthorities, String authorityCode) {
        if(grantedAuthorities == null) {
            return false;
        }

        for(GrantedAuthority grantedAuthority : grantedAuthorities) {
            if (authorityCode.equals(grantedAuthority.getRole())) {
                return true;
            }
        }

        return false;
    }
}

package com.prometis.appbuilder.security.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @AllArgsConstructor @Builder
public class GrantedPermission {
    private String role;

    public static boolean hasPermission(List<GrantedPermission> grantedPermissions, String permissionCode) {
        if(grantedPermissions == null) {
            return false;
        }

        for(GrantedPermission grantedPermission : grantedPermissions) {
            if (permissionCode.equals(grantedPermission.getRole())) {
                return true;
            }
        }

        return false;
    }
}

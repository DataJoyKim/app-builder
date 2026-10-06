package com.prometis.appbuilder.platform.user;

public enum UserAuthority {
    PLATFORM_ADMIN,
    APPLICATION_ADMIN,
    USER,
    ;

    public static boolean contains(String value) {
        for(UserAuthority authority : values()) {
            if(authority.name().equals(value)) return true;
        }
        return false;
    }
}

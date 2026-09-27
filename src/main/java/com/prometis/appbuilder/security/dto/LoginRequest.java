package com.prometis.appbuilder.security.dto;

import lombok.Getter;

@Getter
public class LoginRequest {
    private String loginId;
    private String password;
}

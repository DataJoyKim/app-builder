package com.prometis.appbuilder.app.security.dto;

import lombok.Getter;

@Getter
public class LoginRequest {
    private String loginId;
    private String password;
}

package com.prometis.appbuilder.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @AllArgsConstructor @Builder
public class AuthTokenResponse {
    private String accessToken;
    private String refreshToken;
}

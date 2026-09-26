package com.prometis.appbuilder.app.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @AllArgsConstructor @Builder
public class SignUpResponse {
    private Long id;
    public static SignUpResponse of(User user) {
        return SignUpResponse.builder().id(user.getId()).build();
    }
}

package com.prometis.appbuilder.platform.join;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinVerifyRequest {
    private String verificationKey;
    private String code;
}

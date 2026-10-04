package com.prometis.appbuilder.platform.join;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증코드를 보낸 결과. 화면은 verificationKey 로 인증을 요청하고, expiresInSeconds 로 남은 시간을 보여준다.
 */
@Getter
@Builder
public class JoinRequestResponse {
    private String verificationKey;
    private String email;
    private long expiresInSeconds;
}

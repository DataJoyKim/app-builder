package com.prometis.appbuilder.platform.join;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미 계정이 있는 사람이 로그인한 상태로 초대를 수락한다.
 */
@Getter
@NoArgsConstructor
public class JoinAcceptRequest {
    private String token;
}

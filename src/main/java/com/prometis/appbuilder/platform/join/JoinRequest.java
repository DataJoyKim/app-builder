package com.prometis.appbuilder.platform.join;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가입 정보. 인증코드 메일을 받기 전에 입력한다. 초대 합류에서는 email 대신 초대받은 이메일을 쓴다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequest {
    private String loginId;
    private String userName;
    private String email;
    private String password;
    private String checkPassword;
    // 초대 합류일 때 초대 링크의 token (공개 가입은 비워둔다)
    private String token;
}

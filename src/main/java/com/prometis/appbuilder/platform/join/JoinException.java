package com.prometis.appbuilder.platform.join;

/**
 * 가입 요청/인증이 규칙에 맞지 않을 때. 메시지는 화면에 그대로 보여준다.
 */
public class JoinException extends Exception {
    public JoinException(String message) {
        super(message);
    }
}

package com.prometis.appbuilder.app.security.exception;

import lombok.Getter;

@Getter
public enum SecurityErrorMessage {
    EXPIRED_TOKEN(401, "E-SEC-001", "토큰이 만료되었습니다."),
    INVALID_TOKEN(400, "E-SEC-002", "잘못된 토큰입니다."),
    UNSUPPORTED_TOKEN(400, "E-SEC-003", "지원하지않는 토큰입니다."),
    EMPTY_TOKEN_CLAIMS(400, "E-SEC-004", "잘못된 토큰입니다."),
    DIFF_REFRESH_TOKEN(400, "E-SEC-005", "잘못된 토큰입니다."),
    NOT_FOUND_USER(404, "E-SEC-006", "존재하지않는 사용자입니다."),
    FAULT_PASSWORD(400, "E-SEC-007", "비밀번호가 잘못되었습니다."),
    NOT_LOGIN(401, "E-SEC-008", "로그인되지 않았습니다."),
            ;
    private Integer status;
    private String errorCode;
    private String errorMsg;

    SecurityErrorMessage(int status, String errorCode, String errorMsg) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}

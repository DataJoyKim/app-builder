package com.prometis.appbuilder.app.security;

import com.prometis.core.exception.ErrorMessage;

public enum AppSecurityErrorMessage implements ErrorMessage {
    NOT_FOUND_SESSION(404, "E-SEC-001", "세션이 존재하지않습니다."),
    NOT_PERMISSION_COMPANY(403, "E-SEC-002", "접근하는 회사에 권한이 존재하지않습니다."),
    ;
    private Integer status;
    private String errorCode;
    private String errorMsg;

    AppSecurityErrorMessage(int status, String errorCode, String errorMsg) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    @Override
    public Integer getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return errorCode;
    }

    @Override
    public String getMsg() {
        return errorMsg;
    }
}

package com.prometis.appbuilder.app.user;

import com.prometis.core.exception.ErrorMessage;

public enum UserErrorMessage implements ErrorMessage {
    ALREADY_EXIST_USER(400, "E-USER-001", "이미 존재하는 사용자입니다."),
    DIFFERENT_CHECK_PASSWORD(400, "E-USER-002", "비밀번호가 잘못되었습니다."),
    ;
    private Integer status;
    private String errorCode;
    private String errorMsg;

    UserErrorMessage(int status, String errorCode, String errorMsg) {
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

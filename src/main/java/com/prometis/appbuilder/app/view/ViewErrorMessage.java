package com.prometis.appbuilder.app.view;

import com.prometis.core.exception.ErrorMessage;

public enum ViewErrorMessage implements ErrorMessage {
    NOT_SETTING_PERMISSION(500, "E-VIEW-001", "권한 설정이 되어있지않습니다. 관리자에게 문의해주세요."),
    NOT_HAS_PERMISSIONS(400, "E-VIEW-002", "권한을 가지고있지않습니다."),
    PERMISSION_DENIED(403, "E-VIEW-003", "접근권한이 존재하지않습니다."),
    FAILED_AUTHENTICATION(401, "E-VIEW-004", "인증 실패하였습니다."),
    NOT_FOUND_APP(404, "E-VIEW-005", "애플리케이션이 존재하지않습니다."),
    ;
    private Integer status;
    private String errorCode;
    private String errorMsg;

    ViewErrorMessage(int status, String errorCode, String errorMsg) {
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

package com.prometis.appbuilder.app.view;

import com.prometis.core.exception.ErrorMessage;

public enum MenuErrorMessage implements ErrorMessage {
    NOT_SETTING_AUTHORITY(500, "E-VIEW-001", "권한 설정이 되어있지않습니다. 관리자에게 문의해주세요."),
    NOT_HAS_AUTHORITIES(400, "E-VIEW-002", "권한을 가지고있지않습니다."),
    PERMISSION_DENIED(400, "E-VIEW-003", "접근권한이 존재하지않습니다.");
    private Integer status;
    private String errorCode;
    private String errorMsg;

    MenuErrorMessage(int status, String errorCode, String errorMsg) {
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

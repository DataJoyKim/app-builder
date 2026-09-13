package com.prometis.appbuilder.restapi;

import com.prometis.core.exception.ErrorMessage;

public enum RestApiErrorMessage implements ErrorMessage {
    NOT_FOUND_API(404, "E-RESTAPI-001", "요청한 API 가 존재하지않습니다."),
    METHOD_NOT_ALLOWED(405, "E-RESTAPI-002", "허용되지 않는 HTTP 메소드입니다."),
    INVALID_REQUEST_BODY(400, "E-RESTAPI-003", "요청 본문이 올바른 JSON 형식이 아닙니다."),
    NOT_FOUND_FILE(404, "E-RESTAPI-007", "다운로드할 파일이 없습니다."),
    INVALID_FILE_CONTENT(500, "E-RESTAPI-008", "파일 내용을 읽을 수 없습니다. 파일내용 컬럼의 값과 인코딩 설정을 확인해주세요."),
    ;
    private final Integer status;
    private final String errorCode;
    private final String errorMsg;

    RestApiErrorMessage(int status, String errorCode, String errorMsg) {
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

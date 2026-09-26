package com.prometis.appbuilder.app.restapi;

import lombok.Getter;

/**
 * 요청값이 스키마에 맞지 않거나(런타임 400), 응답이 응답스키마에 맞지 않거나(런타임 500), API 정의가 잘못된 경우(콘솔 저장) 던진다.
 * 어떤 값이 왜 틀렸는지 알려줘야 해서 고정 메시지 enum(ErrorMessage) 대신 메시지를 그때그때 만든다.
 */
@Getter
public class RestApiValidationException extends Exception {
    public static final int STATUS = 400;
    public static final String REQUEST_CODE = "E-RESTAPI-004";
    public static final String DEFINITION_CODE = "E-RESTAPI-005";
    public static final int RESPONSE_STATUS = 500;
    public static final String RESPONSE_CODE = "E-RESTAPI-006";

    private final int status;
    private final String code;

    private RestApiValidationException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static RestApiValidationException request(String message) {
        return new RestApiValidationException(STATUS, REQUEST_CODE, message);
    }

    public static RestApiValidationException definition(String message) {
        return new RestApiValidationException(STATUS, DEFINITION_CODE, message);
    }

    // 워크플로우 결과가 응답스키마와 맞지 않는 경우. 호출한 쪽 잘못이 아니므로 500 이다.
    public static RestApiValidationException response(String message) {
        return new RestApiValidationException(RESPONSE_STATUS, RESPONSE_CODE, message);
    }
}

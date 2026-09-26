package com.prometis.appbuilder.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter @AllArgsConstructor @Builder
public class ResponseMessage {
    private Map<String, List<Map<String, Object>>> contents;
    private Integer status;
    private String message;
    private String code;
    private ResultType resultType;

    public static ResponseMessage createSuccessMessage(Map<String, List<Map<String, Object>>> contents) {
        return ResponseMessage.builder()
                .contents(contents)
                .status(200)
                .code("S001")
                .resultType(ResultType.SUCCESS)
                .message("처리완료 되었습니다.")
                .build();
    }

    public static ResponseMessage createErrorMessage(Integer status, String errorCode, String errorMsg) {
        return ResponseMessage.builder()
                .status(status)
                .code(errorCode)
                .resultType(ResultType.ERROR)
                .message(errorMsg)
                .build();
    }

    public static ResponseMessage createErrorMessage(Integer status, String errorCode, String errorMsg, Map<String, List<Map<String, Object>>> contents) {
        return ResponseMessage.builder()
                .status(status)
                .code(errorCode)
                .resultType(ResultType.ERROR)
                .message(errorMsg)
                .contents(contents)
                .build();
    }
}

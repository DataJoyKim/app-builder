package com.prometis.appbuilder.app.message;

import com.prometis.appbuilder.app.message.code.MessageProcessorResultCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Builder @AllArgsConstructor
public class MessageProcessorResult {
    private MessageProcessorResultCode resultCode;
    private Object content;

    public static MessageProcessorResult createSuccessMessage(Object content) {
        return MessageProcessorResult.builder()
                .resultCode(MessageProcessorResultCode.SUCCESS)
                .content(content)
                .build();
    }

    public static MessageProcessorResult createErrorMessage(String errorCode, String errorMessage) {
        List<Map<String, Object>> resultData = new ArrayList<>();
        Map<String, Object> error = new HashMap<>();
        error.put("errorCode", errorCode);
        error.put("message",errorMessage);
        resultData.add(error);

        return MessageProcessorResult.builder()
                .resultCode(MessageProcessorResultCode.FAILURE)
                .content(resultData)
                .build();
    }
}

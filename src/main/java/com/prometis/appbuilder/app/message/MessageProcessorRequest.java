package com.prometis.appbuilder.app.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter @AllArgsConstructor @Builder
public class MessageProcessorRequest {
    private List<Map<String, Object>> contents;
}

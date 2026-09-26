package com.prometis.appbuilder.app.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter @AllArgsConstructor @Builder
public class EntityRequest {
    private List<Map<String, Object>> contents;
}

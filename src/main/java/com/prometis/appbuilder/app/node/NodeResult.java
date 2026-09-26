package com.prometis.appbuilder.app.node;

import com.prometis.appbuilder.app.node.code.ResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter @AllArgsConstructor @Builder
public class NodeResult {
    private ResultType resultType;
    private List<Map<String, Object>> results;
}

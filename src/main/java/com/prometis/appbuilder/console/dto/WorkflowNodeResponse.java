package com.prometis.appbuilder.console.dto;

import com.prometis.appbuilder.app.node.code.ErrorResolveType;
import com.prometis.appbuilder.app.node.code.FunctionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @AllArgsConstructor @Builder
public class WorkflowNodeResponse {
    private Long id;
    private Long workflowId;
    private String nodeId;
    private String functionName;
    private String displayName;
    private FunctionType functionType;
    private ErrorResolveType errorResolveType;
    private Integer orderNum;
    private Boolean isLogging;
    private String requestMessageId;
    private String responseMessageId;
    private Integer positionX;
    private Integer positionY;

}

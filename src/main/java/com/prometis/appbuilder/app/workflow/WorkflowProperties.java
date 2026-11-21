package com.prometis.appbuilder.app.workflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 platform.workflow 설정.
 *
 * @param maxExecuteStep 워크플로우 한 번 실행에서 거칠 수 있는 최대 노드 수. 노드 연결이 돌고 돌 때 끝없이 실행되지 않도록 막는다.
 *                       워크플로우 실행 노드로 불린 워크플로우는 자기 실행분을 따로 센다
 * @param maxCallDepth   워크플로우 실행 노드로 겹쳐 부를 수 있는 최대 깊이. 처음 호출된 워크플로우를 1 로 센다
 */
@ConfigurationProperties(prefix = "platform.workflow")
public record WorkflowProperties(Integer maxExecuteStep, Integer maxCallDepth) {
    public static final int DEFAULT_MAX_EXECUTE_STEP = 1000;
    public static final int DEFAULT_MAX_CALL_DEPTH = 10;

    public WorkflowProperties {
        if(maxExecuteStep == null || maxExecuteStep < 1) {
            maxExecuteStep = DEFAULT_MAX_EXECUTE_STEP;
        }
        if(maxCallDepth == null || maxCallDepth < 1) {
            maxCallDepth = DEFAULT_MAX_CALL_DEPTH;
        }
    }

    public static WorkflowProperties defaults() {
        return new WorkflowProperties(null, null);
    }
}

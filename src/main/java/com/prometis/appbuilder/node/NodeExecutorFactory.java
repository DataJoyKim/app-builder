package com.prometis.appbuilder.node;

import com.prometis.appbuilder.node.code.FunctionType;
import com.prometis.appbuilder.node.executor.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NodeExecutorFactory {

    private final EntityExecutor entityExecutor;
    private final QueryExecutor queryExecutor;
    private final RestClientExecutor restClientExecutor;
    private final MessageProcessorExecutor messageProcessorExecutor;
    private final NotificationExecutor notificationExecutor;
    private final FileExecutor fileExecutor;

    public NodeExecutor instance(FunctionType functionType) {
        return switch (functionType) {
            case ENTITY -> entityExecutor;
            case SQL -> queryExecutor;
            case REST_CLIENT -> restClientExecutor;
            case MESSAGE_PROCESSOR -> messageProcessorExecutor;
            case NOTIFICATION -> notificationExecutor;
            case FILE -> fileExecutor;
            // CONDITION 은 데이터를 만들어내는 기능이 아니라 흐름을 가르는 제어 노드라서
            // FunctionExecutor 가 아니라 WorkflowService 가 직접 판정한다.
            case CONDITION -> throw new IllegalArgumentException("CONDITION 노드는 FunctionExecutor 로 실행할 수 없습니다.");
            // ERROR_MESSAGE 도 흐름을 끝내고 에러응답을 돌려주는 제어 노드라 WorkflowService 가 직접 처리한다.
            case ERROR_MESSAGE -> throw new IllegalArgumentException("ERROR_MESSAGE 노드는 FunctionExecutor 로 실행할 수 없습니다.");
        };
    }
}

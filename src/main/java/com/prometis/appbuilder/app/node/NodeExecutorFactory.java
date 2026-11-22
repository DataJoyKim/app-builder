package com.prometis.appbuilder.app.node;

import com.prometis.appbuilder.app.node.code.FunctionType;
import com.prometis.appbuilder.app.node.executor.*;
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
    private final PluginExecutor pluginExecutor;

    public NodeExecutor instance(FunctionType functionType) {
        return switch (functionType) {
            case ENTITY -> entityExecutor;
            case SQL -> queryExecutor;
            case REST_CLIENT -> restClientExecutor;
            case MESSAGE_PROCESSOR -> messageProcessorExecutor;
            case NOTIFICATION -> notificationExecutor;
            case FILE -> fileExecutor;
            case PLUGIN -> pluginExecutor;
            case WORKFLOW -> throw new IllegalArgumentException("WORKFLOW 노드는 FunctionExecutor 로 실행할 수 없습니다.");
            case CONDITION -> throw new IllegalArgumentException("CONDITION 노드는 FunctionExecutor 로 실행할 수 없습니다.");
            case ERROR_MESSAGE -> throw new IllegalArgumentException("ERROR_MESSAGE 노드는 FunctionExecutor 로 실행할 수 없습니다.");
        };
    }
}

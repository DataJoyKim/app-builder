package com.prometis.appbuilder.app.node.executor;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.node.NodeConfig;
import com.prometis.appbuilder.app.node.NodeExecutor;
import com.prometis.appbuilder.app.node.NodeResult;
import com.prometis.appbuilder.app.node.code.ResultType;
import com.prometis.appbuilder.app.plugin.PluginService;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.plugin.api.PluginRequest;
import com.prometis.plugin.api.PluginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PLUGIN 노드. 기능코드(functionName)가 pluginPath 이고, 요청메시지(IN) 행 전체를 한 번에 플러그인으로 넘긴 뒤
 * 플러그인이 돌려준 행들을 응답메시지(OUT)에 담는다.
 */
@Component
@RequiredArgsConstructor
public class PluginExecutor implements NodeExecutor {
    private final PluginService pluginService;
    private final NodeConfig config;

    @Override
    public NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(PluginRequest.ATTR_FUNCTION_NAME, functionName);
        if(user != null) {
            attributes.put(PluginRequest.ATTR_USER_ID, user.getUserId());
            attributes.put(PluginRequest.ATTR_USER_NAME, user.getUserName());
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        if(params != null) {
            for(Map<String, Object> param : params) {
                contents.add(param == null ? new HashMap<>() : new HashMap<>(param));
            }
        }

        PluginResult result = pluginService.execute(header.getApplicationId(), functionName, new PluginRequest(contents, attributes));

        List<Map<String, Object>> results = new ArrayList<>();
        for(Map<String, Object> row : result.getContents()) {
            Map<String, Object> responseObj = (row == null) ? new HashMap<>() : new HashMap<>(row);
            responseObj.putIfAbsent(config.getRequestMessageSeqKey(), null);
            results.add(responseObj);
        }

        if(!result.isSuccess()) {
            Map<String, Object> failure = new HashMap<>();
            failure.put(config.getRequestMessageSeqKey(), null);
            failure.put("resultCode", ResultType.FAILURE.name());
            failure.put("message", result.getMessage());
            results.add(failure);
        }

        return NodeResult.builder()
                .resultType(result.isSuccess() ? ResultType.SUCCESS : ResultType.FAILURE)
                .results(results)
                .build();
    }
}

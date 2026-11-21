package com.template.handler;

import com.prometis.plugin.api.PluginContext;
import com.prometis.plugin.api.PluginRequest;
import com.prometis.plugin.api.PluginResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * pluginPath: template/echo
 *
 * <p>요청메시지 행마다 받은 값을 그대로 돌려주고, 호출 정보(애플리케이션ID, 로그인 사용자)를 덧붙인다.
 * 요청메시지와 응답메시지가 어떻게 오가는지 확인하는 용도.</p>
 */
public class EchoHandler implements PathHandler {

    @Override
    public PluginResult handle(PluginRequest request, PluginContext context) {
        List<Map<String, Object>> results = new ArrayList<>();

        for(Map<String, Object> row : request.getContents()) {
            Map<String, Object> result = new LinkedHashMap<>(row);
            result.put("applicationId", context.getApplicationId());
            result.put("userId", request.getAttribute(PluginRequest.ATTR_USER_ID));
            result.put("handledAt", LocalDateTime.now().toString());
            results.add(result);
        }

        return PluginResult.success(results);
    }
}

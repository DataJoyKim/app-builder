package com.prometis.appbuilder.app.node.executor;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.executor.notification.SendResultType;
import com.prometis.appbuilder.app.node.NodeConfig;
import com.prometis.appbuilder.app.node.NodeExecutor;
import com.prometis.appbuilder.app.node.NodeResult;
import com.prometis.appbuilder.app.node.code.ResultType;
import com.prometis.appbuilder.app.notification.NotificationRequest;
import com.prometis.appbuilder.app.notification.NotificationResult;
import com.prometis.appbuilder.app.notification.NotificationService;
import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationExecutor implements NodeExecutor {
    private final NotificationService notificationService;
    private final NodeConfig config;

    @Override
    public NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params) {

        ResultType resultType = ResultType.SUCCESS;
        List<Map<String, Object>> results = new ArrayList<>();

        for(Map<String, Object> param : params) {
            String seq = (String) param.get(config.getRequestMessageSeqKey());

            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .params(param)
                    .build();

            NotificationResult result = notificationService.execute(functionName, notificationRequest);

            if(result.getResultCode() == SendResultType.FAILURE) {
                resultType = ResultType.FAILURE;
            }

            Map<String, Object> responseObj = new HashMap<>();
            responseObj.put(config.getRequestMessageSeqKey(), seq);
            responseObj.put("message", result.getMessage());
            responseObj.put("resultCode", result.getResultCode());

            results.add(responseObj);
        }

        return NodeResult.builder()
                .resultType(resultType)
                .results(results)
                .build();
    }
}

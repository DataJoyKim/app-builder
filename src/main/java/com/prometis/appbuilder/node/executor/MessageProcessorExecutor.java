package com.prometis.appbuilder.node.executor;

import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.appbuilder.message.MessageProcessorRequest;
import com.prometis.appbuilder.message.MessageProcessorResult;
import com.prometis.appbuilder.message.MessageProcessorService;
import com.prometis.appbuilder.message.code.MessageProcessorResultCode;
import com.prometis.appbuilder.node.NodeConfig;
import com.prometis.appbuilder.node.NodeExecutor;
import com.prometis.appbuilder.node.NodeResult;
import com.prometis.appbuilder.node.code.ResultType;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessageProcessorExecutor implements NodeExecutor {
    private final MessageProcessorService messageProcessorService;
    private final NodeConfig config;

    @Override
    public NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params) {

        MessageProcessorRequest request = MessageProcessorRequest.builder()
                .contents(params)
                .build();

        MessageProcessorResult result = messageProcessorService.execute(functionName, request);

        List<Map<String,Object>> results = new ArrayList<>();
        Object resultObj = result.getContent();
        if(resultObj instanceof List) {
            List<Map<String, Object>> responseArr = (List<Map<String, Object>>) resultObj;
            for(Map<String,Object> responseObj : responseArr) {
                responseObj.put(config.getRequestMessageSeqKey(), null);
            }
            results.addAll(responseArr);
        }
        else if(resultObj instanceof Map) {
            Map<String, Object> responseObj = (Map<String, Object>) resultObj;
            responseObj.put(config.getRequestMessageSeqKey(), null);
            results.add(responseObj);
        }

        return NodeResult.builder()
                .resultType(MessageProcessorResultCode.SUCCESS.equals(result.getResultCode()) ? ResultType.SUCCESS : ResultType.FAILURE)
                .results(results)
                .build();
    }
}

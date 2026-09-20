package com.prometis.appbuilder.node.executor;

import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.appbuilder.node.NodeConfig;
import com.prometis.appbuilder.node.NodeExecutor;
import com.prometis.appbuilder.node.NodeResult;
import com.prometis.appbuilder.node.code.ResultType;
import com.prometis.appbuilder.restclient.RestClientRequest;
import com.prometis.appbuilder.restclient.RestClientResult;
import com.prometis.appbuilder.restclient.RestClientService;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("function.RestClientExecutor")
@RequiredArgsConstructor
public class RestClientExecutor implements NodeExecutor {
    private final RestClientService restClientService;
    private final NodeConfig config;

    @Override
    public NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params) {

        ResultType resultType = ResultType.SUCCESS;
        List<Map<String, Object>> results = new ArrayList<>();

        for(Map<String, Object> param : params) {
            String seq = (String) param.get(config.getRequestMessageSeqKey());

            RestClientRequest restClientParams = RestClientRequest.builder()
                    .params(param)
                    .requestBody(param.get(config.getRequestMessageRestClientRequestBodyKey()))
                    .build();

            RestClientResult restClientResult = restClientService.execute(functionName, restClientParams);

            if(restClientResult.getStatusCode().isError()) {
                resultType = ResultType.FAILURE;
            }

            Object response = restClientResult.getBody();

            if(response instanceof List) {
                List<Map<String, Object>> responseArr = (List<Map<String, Object>>) response;
                for(Map<String, Object> obj : responseArr) {
                    obj.put(config.getRequestMessageSeqKey(), seq);
                }

                results.addAll(responseArr);
            }
            else if(response instanceof Map) {
                Map<String, Object> responseObj = (Map<String, Object>) response;
                responseObj.put(config.getRequestMessageSeqKey(), seq);

                results.add(responseObj);
            }
        }

        return NodeResult.builder()
                .resultType(resultType)
                .results(results)
                .build();
    }
}

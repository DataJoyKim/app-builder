package com.prometis.appbuilder.node.executor;

import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.appbuilder.node.NodeConfig;
import com.prometis.appbuilder.node.NodeExecutor;
import com.prometis.appbuilder.node.NodeResult;
import com.prometis.appbuilder.node.code.ResultType;
import com.prometis.appbuilder.query.QueryRequest;
import com.prometis.appbuilder.query.QueryResult;
import com.prometis.appbuilder.query.QueryService;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QueryExecutor implements NodeExecutor {
    private final QueryService queryService;
    private final NodeConfig config;

    @Override
    public NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params) {
        ResultType resultType = ResultType.SUCCESS;
        List<Map<String,Object>> results = new ArrayList<>();

        for(Map<String, Object> param : params) {
            String seq = String.valueOf(param.get(config.getRequestMessageSeqKey()));

            QueryRequest queryParams = QueryRequest.builder()
                                        .contents(param)
                                        .build();

            QueryResult queryResults = queryService.execute(functionName, queryParams);

            for(Map<String,Object> result : queryResults.getResults()) {
                result.put(config.getRequestMessageSeqKey(), seq);
            }

            results.addAll(queryResults.getResults());
        }

        return NodeResult.builder()
                .resultType(resultType)
                .results(results)
                .build();
    }
}

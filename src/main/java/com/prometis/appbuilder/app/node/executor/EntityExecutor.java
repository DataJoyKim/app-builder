package com.prometis.appbuilder.app.node.executor;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.entity.EntityRequest;
import com.prometis.appbuilder.app.entity.EntityResult;
import com.prometis.appbuilder.app.entity.EntityService;
import com.prometis.appbuilder.app.entity.code.EntityResultCode;
import com.prometis.appbuilder.app.node.NodeExecutor;
import com.prometis.appbuilder.app.node.NodeResult;
import com.prometis.appbuilder.app.node.code.ResultType;
import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EntityExecutor implements NodeExecutor {
    private final EntityService entityService;

    @Override
    public NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params) {
        EntityRequest entityParams = EntityRequest.builder()
                                        .contents(params)
                                        .build();

        EntityResult results = entityService.execute(functionName, entityParams);

        return NodeResult.builder()
                .resultType(EntityResultCode.SUCCESS.equals(results.getResultCode()) ? ResultType.SUCCESS : ResultType.FAILURE)
                .results(results.getResults())
                .build();
    }
}

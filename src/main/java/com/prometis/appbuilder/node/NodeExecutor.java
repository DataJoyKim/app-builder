package com.prometis.appbuilder.node;

import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;

import java.util.List;
import java.util.Map;

public interface NodeExecutor {
    NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params);
}

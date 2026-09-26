package com.prometis.appbuilder.app.node;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.security.domain.AuthenticatedUser;

import java.util.List;
import java.util.Map;

public interface NodeExecutor {
    NodeResult execute(AuthenticatedUser user, String functionName, RequestMessage.Header header, List<Map<String, Object>> params);
}

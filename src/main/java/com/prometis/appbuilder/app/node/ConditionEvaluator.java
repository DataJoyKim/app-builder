package com.prometis.appbuilder.app.node;

import com.prometis.appbuilder.app.executor.script.ScriptEngine;
import com.prometis.appbuilder.app.executor.script.ScriptEngineExecuteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 조건분기(CONDITION) 노드의 판정식을 실행한다.
 * 판정식은 JS 이며, 노드의 요청메시지ID 슬롯을 params 로 참조한다.
 * 예) params[0].status === 'OK'
 */
@Component
@RequiredArgsConstructor
public class ConditionEvaluator {
    private static final Pattern RETURN_KEYWORD = Pattern.compile("\\breturn\\b");

    private final ScriptEngine scriptEngine;

    public boolean evaluate(String conditionExpression, List<Map<String, Object>> params) throws ScriptEngineExecuteException {
        if(conditionExpression == null || conditionExpression.isBlank()) {
            return false;
        }

        Object result = scriptEngine.execute(createScript(conditionExpression), params);

        return toBoolean(result);
    }

    // 한 줄짜리 판정식(status === 'OK')과 여러 줄 스크립트(return ...) 를 모두 지원한다.
    private String createScript(String conditionExpression) {
        String expression = conditionExpression.trim();

        if(RETURN_KEYWORD.matcher(expression).find()) {
            return expression;
        }

        return "return (" + expression + ");";
    }

    // JS 의 truthy 판정과 동일하게 맞춘다.
    private boolean toBoolean(Object result) {
        if(result == null) {
            return false;
        }

        if(result instanceof Boolean) {
            return (Boolean) result;
        }

        if(result instanceof Number) {
            return ((Number) result).doubleValue() != 0d;
        }

        if(result instanceof String) {
            return !((String) result).isEmpty();
        }

        if(result instanceof List) {
            return !((List<?>) result).isEmpty();
        }

        if(result instanceof Map) {
            return true;
        }

        return true;
    }
}

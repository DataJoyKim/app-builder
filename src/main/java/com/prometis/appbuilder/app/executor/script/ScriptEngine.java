package com.prometis.appbuilder.app.executor.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ScriptEngine {
    public Object execute(String script, List<Map<String, Object>> params) throws ScriptEngineExecuteException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String paramsJson = objectMapper.writeValueAsString(params == null ? Collections.emptyList() : params);

            String languageId = "js";

            try (Context context = Context.newBuilder(languageId)
                    .option("engine.WarnInterpreterOnly", "false") // GraalVM 사용안함으로 false 처리
                    .allowAllAccess(false).build()
            ) {
                // 요청메시지 Array 변환
                context.getBindings(languageId)
                        .putMember("paramsJson", paramsJson);

                Value jsParams = context.eval(languageId, "JSON.parse(paramsJson)");

                context.getBindings(languageId)
                        .putMember("params", jsParams);

                // 함수로 감싸서 실행
                String wrappedScript = """
                        (function(params) {
                            %s
                        })(params)
                        """.formatted(script);

                Value result = context.eval(languageId, wrappedScript);

                return convertValue(result);
            }

        } catch (Exception e) {
            throw new ScriptEngineExecuteException(e);
        }
    }

    private Object convertValue(Value value) {
        if (value.isNull()) {
            return null;
        }

        if (value.isString()) {
            return value.asString();
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        if (value.isNumber()) {
            return value.as(Number.class);
        }

        if (value.hasArrayElements()) {
            List<Object> result = new ArrayList<>();

            for (long i = 0; i < value.getArraySize(); i++) {
                result.add(convertValue(value.getArrayElement(i)));
            }

            return result;
        }

        if (value.hasMembers()) {
            Map<String, Object> result = new LinkedHashMap<>();

            for (String key : value.getMemberKeys()) {
                result.put(key, convertValue(value.getMember(key)));
            }

            return result;
        }

        return value.as(Object.class);
    }
}

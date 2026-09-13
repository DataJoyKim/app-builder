package com.prometis.appbuilder.restapi;

import com.prometis.appbuilder.restapi.code.ParameterIn;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.function.Function;

/**
 * 들어온 요청에서 파라미터 값을 꺼내는 창구. 서블릿 요청에 직접 묶지 않아서 매핑 로직을 따로 검증할 수 있다.
 */
@Getter
@Builder
public class RestApiRequestData {
    private final Map<String, String> pathVariables;
    private final Map<String, String[]> queryParameters;
    private final Function<String, String> headerReader;
    // JSON 을 파싱한 결과(Map / List / 기본값). 본문이 없으면 null.
    private final Object body;

    public String valueOf(ParameterIn paramIn, String name) {
        return switch (paramIn) {
            case PATH -> pathVariables == null ? null : pathVariables.get(name);
            case QUERY -> {
                String[] values = queryParameters == null ? null : queryParameters.get(name);
                yield (values == null || values.length == 0) ? null : values[0];
            }
            case HEADER -> headerReader == null ? null : headerReader.apply(name);
            case BODY -> throw new IllegalArgumentException("BODY 파라미터는 본문 트리에서 읽는다.");
            case RESPONSE -> throw new IllegalArgumentException("RESPONSE 는 요청에서 읽는 값이 아니다.");
        };
    }
}

package com.prometis.appbuilder.restapi;

import com.prometis.appbuilder.restapi.code.HttpMethodType;
import com.prometis.appbuilder.restapi.code.ParameterIn;
import com.prometis.appbuilder.restapi.code.SchemaDataType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RestApiRequestMapperTest {
    private final RestApiRequestMapper mapper = new RestApiRequestMapper();

    private int seq = 0;

    private RestApiParameter param(ParameterIn paramIn, String name, SchemaDataType dataType, boolean required, String messageId) {
        return param(paramIn, "p" + (++seq), null, name, dataType, required, messageId, null, null);
    }

    private RestApiParameter param(
            ParameterIn paramIn, String paramKey, String parentKey, String name, SchemaDataType dataType,
            boolean required, String messageId, String mappingKey, String defaultValue
    ) {
        return RestApiParameter.builder()
                .restApiId(1L)
                .paramIn(paramIn)
                .paramKey(paramKey)
                .parentKey(parentKey)
                .name(name)
                .dataType(dataType)
                .required(required)
                .messageId(messageId)
                .mappingKey(mappingKey)
                .defaultValue(defaultValue)
                .orderNum(++seq)
                .build();
    }

    private RestApi api(HttpMethodType method, SchemaDataType bodyDataType, String bodyMessageId) {
        return RestApi.builder()
                .id(1L)
                .apiCode("TEST")
                .displayName("테스트")
                .httpMethod(method)
                .path("/test")
                .workflowCode("WF")
                .bodyDataType(bodyDataType)
                .bodyMessageId(bodyMessageId)
                .enabled(true)
                .build();
    }

    private RestApiRequestData request(Map<String, String> path, Map<String, String> query, Map<String, String> headers, Object body) {
        Map<String, String[]> queryParameters = new HashMap<>();
        query.forEach((key, value) -> queryParameters.put(key, new String[]{value}));

        return RestApiRequestData.builder()
                .pathVariables(path)
                .queryParameters(queryParameters)
                .headerReader(name -> headers.entrySet().stream()
                        .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null))
                .body(body)
                .build();
    }

    @Test
    public void 경로변수_쿼리_헤더는_타입을_바꿔_지정한_요청메시지에_담긴다() throws Exception {
        List<RestApiParameter> parameters = List.of(
                param(ParameterIn.PATH, "p1", null, "evalId", SchemaDataType.INTEGER, true, "ME_IN", null, null),
                param(ParameterIn.QUERY, "p2", null, "active", SchemaDataType.BOOLEAN, false, "ME_IN", null, null),
                param(ParameterIn.QUERY, "p3", null, "page", SchemaDataType.INTEGER, false, "ME_PAGE", null, "1"),
                param(ParameterIn.HEADER, "p4", null, "X-User-Id", SchemaDataType.STRING, true, "ME_IN", "user_id", null),
                // 정의만 하고 보내지 않은 선택 파라미터는 null 로 담긴다.
                param(ParameterIn.QUERY, "p5", null, "keyword", SchemaDataType.STRING, false, "ME_IN", null, null)
        );

        Map<String, List<Map<String, Object>>> messages = mapper.map(
                api(HttpMethodType.GET, null, null),
                parameters,
                request(Map.of("evalId", "15"), Map.of("active", "TRUE", "ignored", "x"), Map.of("x-user-id", "U001"), null)
        );

        assertEquals(Set.of("ME_IN", "ME_PAGE"), messages.keySet());

        Map<String, Object> in = messages.get("ME_IN").get(0);
        assertEquals(1, messages.get("ME_IN").size());
        assertEquals(15, in.get("evalId"));
        assertEquals(true, in.get("active"));
        assertEquals("U001", in.get("user_id"));
        assertTrue(in.containsKey("keyword"));
        assertNull(in.get("keyword"));
        assertFalse(in.containsKey("ignored"));

        // 값이 없으면 기본값을 쓴다.
        assertEquals(1, messages.get("ME_PAGE").get(0).get("page"));
    }

    @Test
    public void 필수값이_없거나_타입이_맞지_않으면_어느_파라미터인지_알려준다() {
        List<RestApiParameter> parameters = List.of(
                param(ParameterIn.QUERY, "page", SchemaDataType.INTEGER, true, "ME_IN")
        );
        RestApi restApi = api(HttpMethodType.GET, null, null);

        RestApiValidationException missing = assertThrows(RestApiValidationException.class,
                () -> mapper.map(restApi, parameters, request(Map.of(), Map.of(), Map.of(), null)));
        assertEquals("Request Param 'page' 는 필수값입니다.", missing.getMessage());
        assertEquals(RestApiValidationException.REQUEST_CODE, missing.getCode());

        RestApiValidationException empty = assertThrows(RestApiValidationException.class,
                () -> mapper.map(restApi, parameters, request(Map.of(), Map.of("page", ""), Map.of(), null)));
        assertEquals("Request Param 'page' 는 필수값입니다.", empty.getMessage());

        RestApiValidationException type = assertThrows(RestApiValidationException.class,
                () -> mapper.map(restApi, parameters, request(Map.of(), Map.of("page", "1.5"), Map.of(), null)));
        assertEquals("Request Param 'page' 는 INTEGER 타입이어야 합니다.", type.getMessage());
    }

    @Test
    public void 본문_객체의_필드는_한_행으로_담기고_배열_필드는_별도_요청메시지의_여러_행이_된다() throws Exception {
        List<RestApiParameter> parameters = List.of(
                param(ParameterIn.PATH, "p1", null, "evalId", SchemaDataType.INTEGER, true, "ME_GOALS", null, null),
                param(ParameterIn.BODY, "b1", null, "title", SchemaDataType.STRING, true, null, null, null),
                param(ParameterIn.BODY, "b2", null, "score", SchemaDataType.NUMBER, false, null, null, null),
                // messageId 가 없는 OBJECT 는 상위 행에 중첩된 값으로 담긴다.
                param(ParameterIn.BODY, "b3", null, "owner", SchemaDataType.OBJECT, false, null, null, null),
                param(ParameterIn.BODY, "b4", "b3", "empId", SchemaDataType.STRING, true, null, "emp_id", null),
                // messageId 가 있는 ARRAY 는 원소마다 그 요청메시지의 한 행이 된다.
                param(ParameterIn.BODY, "b5", null, "goals", SchemaDataType.ARRAY, true, "ME_GOALS", null, null),
                param(ParameterIn.BODY, "b6", "b5", "goalId", SchemaDataType.INTEGER, true, null, "goal_id", null),
                param(ParameterIn.BODY, "b7", "b5", "weight", SchemaDataType.NUMBER, false, null, null, "0"),
                // messageId 가 없는 기본값 배열은 그대로 담긴다.
                param(ParameterIn.BODY, "b8", null, "tags", SchemaDataType.ARRAY, false, null, null, null)
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "상반기 평가");
        body.put("score", "87.5");
        body.put("owner", Map.of("empId", "E01", "extra", "넘기지 않음"));
        body.put("goals", List.of(Map.of("goalId", 1, "weight", 30), Map.of("goalId", "2")));
        body.put("tags", List.of("a", "b"));
        body.put("undefined", "넘기지 않음");

        Map<String, List<Map<String, Object>>> messages = mapper.map(
                api(HttpMethodType.POST, SchemaDataType.OBJECT, "ME_EVAL"),
                parameters,
                request(Map.of("evalId", "7"), Map.of(), Map.of(), body)
        );

        Map<String, Object> eval = messages.get("ME_EVAL").get(0);
        assertEquals(1, messages.get("ME_EVAL").size());
        assertEquals("상반기 평가", eval.get("title"));
        assertEquals(new BigDecimal("87.5"), eval.get("score"));
        assertEquals(Map.of("emp_id", "E01"), eval.get("owner"));
        assertEquals(List.of("a", "b"), eval.get("tags"));
        assertFalse(eval.containsKey("goals"));
        assertFalse(eval.containsKey("undefined"));

        // 같은 요청메시지에 담긴 경로변수(기본 행 값)는 배열의 모든 행에 채워진다.
        List<Map<String, Object>> goals = messages.get("ME_GOALS");
        assertEquals(2, goals.size());
        assertEquals(Map.of("goal_id", 1, "weight", 30, "evalId", 7), goals.get(0));
        // 문자열로 온 숫자는 타입에 맞게 바뀌고, 빠진 값은 기본값("0" -> NUMBER)으로 채워진다.
        assertEquals(Map.of("goal_id", 2, "weight", new BigDecimal("0"), "evalId", 7), goals.get(1));
    }

    @Test
    public void 본문_최상위가_배열이면_원소마다_한_행이_된다() throws Exception {
        List<RestApiParameter> parameters = List.of(
                param(ParameterIn.BODY, "b1", null, "id", SchemaDataType.INTEGER, true, null, null, null)
        );
        RestApi restApi = api(HttpMethodType.DELETE, SchemaDataType.ARRAY, "ME_IDS");

        Map<String, List<Map<String, Object>>> messages = mapper.map(
                restApi, parameters, request(Map.of(), Map.of(), Map.of(), List.of(Map.of("id", 1), Map.of("id", 2)))
        );
        assertEquals(List.of(Map.of("id", 1), Map.of("id", 2)), messages.get("ME_IDS"));

        // 본문이 없으면 빈 배열로 본다.
        assertEquals(List.of(), mapper.map(restApi, parameters, request(Map.of(), Map.of(), Map.of(), null)).get("ME_IDS"));

        RestApiValidationException notArray = assertThrows(RestApiValidationException.class,
                () -> mapper.map(restApi, parameters, request(Map.of(), Map.of(), Map.of(), Map.of("id", 1))));
        assertEquals("Request Body 는 배열(ARRAY)이어야 합니다.", notArray.getMessage());

        RestApiValidationException elementError = assertThrows(RestApiValidationException.class,
                () -> mapper.map(restApi, parameters, request(Map.of(), Map.of(), Map.of(), List.of(Map.of("id", 1), Map.of("id", "x")))));
        assertEquals("Request Body 'body[1].id' 는 INTEGER 타입이어야 합니다.", elementError.getMessage());
    }

    @Test
    public void 본문_중첩_필드의_에러는_위치를_경로로_알려준다() {
        List<RestApiParameter> parameters = List.of(
                param(ParameterIn.BODY, "b1", null, "goals", SchemaDataType.ARRAY, true, "ME_GOALS", null, null),
                param(ParameterIn.BODY, "b2", "b1", "goalId", SchemaDataType.INTEGER, true, null, null, null)
        );
        RestApi restApi = api(HttpMethodType.POST, SchemaDataType.OBJECT, "ME_EVAL");

        RestApiValidationException missingArray = assertThrows(RestApiValidationException.class,
                () -> mapper.map(restApi, parameters, request(Map.of(), Map.of(), Map.of(), Map.of())));
        assertEquals("Request Body 'body.goals' 는 필수값입니다.", missingArray.getMessage());

        RestApiValidationException missingField = assertThrows(RestApiValidationException.class,
                () -> mapper.map(restApi, parameters, request(Map.of(), Map.of(), Map.of(),
                        Map.of("goals", List.of(Map.of("goalId", 1), Map.of())))));
        assertEquals("Request Body 'body.goals[1].goalId' 는 필수값입니다.", missingField.getMessage());

        RestApiValidationException wrongType = assertThrows(RestApiValidationException.class,
                () -> mapper.map(restApi, parameters, request(Map.of(), Map.of(), Map.of(), Map.of("goals", "x"))));
        assertEquals("Request Body 'body.goals' 는 배열(ARRAY)이어야 합니다.", wrongType.getMessage());
    }

    @Test
    public void 하위_필드를_정의하지_않은_본문은_들어온_그대로_넘긴다() throws Exception {
        Map<String, List<Map<String, Object>>> messages = mapper.map(
                api(HttpMethodType.PUT, SchemaDataType.OBJECT, "ME_RAW"),
                List.of(),
                request(Map.of(), Map.of(), Map.of(), Map.of("a", 1, "b", List.of(1, 2)))
        );

        assertEquals(Map.of("a", 1, "b", List.of(1, 2)), messages.get("ME_RAW").get(0));
    }
}

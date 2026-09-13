package com.prometis.appbuilder.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.appbuilder.dto.ResponseMessage;
import com.prometis.appbuilder.restapi.code.HttpMethodType;
import com.prometis.appbuilder.workflow.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 콘솔 저장 경로(정의 검증 포함)와, 저장한 정의로 실제 요청을 받아 워크플로우 요청메시지를 만드는 경로를 함께 확인한다.
 */
@DataJpaTest
@Import({RestApiService.class, RestApiRequestMapper.class})
class RestApiServiceTest {
    @Autowired
    private RestApiService restApiService;
    @Autowired
    private RestApiRepository restApiRepository;
    @Autowired
    private RestApiParameterRepository restApiParameterRepository;
    @Autowired
    private RestApiRequestMapper restApiRequestMapper;

    private Map<String, Object> restApi(String apiCode, String method, String path, String bodyDataType, String bodyMessageId) {
        Map<String, Object> restApi = new HashMap<>();
        restApi.put("id", "");
        restApi.put("apiCode", apiCode);
        restApi.put("displayName", apiCode + " 이름");
        restApi.put("httpMethod", method);
        restApi.put("path", path);
        restApi.put("workflowCode", "WF_" + apiCode);
        restApi.put("bodyDataType", bodyDataType);
        restApi.put("bodyMessageId", bodyMessageId);
        restApi.put("enabled", true);
        return restApi;
    }

    private Map<String, Object> parameter(String paramIn, String paramKey, String parentKey, String name, String dataType,
                                          boolean required, String messageId) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("paramIn", paramIn);
        parameter.put("paramKey", paramKey);
        parameter.put("parentKey", parentKey);
        parameter.put("name", name);
        parameter.put("dataType", dataType);
        parameter.put("required", required);
        parameter.put("messageId", messageId);
        parameter.put("mappingKey", "");
        parameter.put("defaultValue", "");
        return parameter;
    }

    private Map<String, Object> saveParams(Map<String, Object> restApi, List<Map<String, Object>> parameters) {
        Map<String, Object> params = new HashMap<>();
        params.put("restApi", restApi);
        params.put("parameters", parameters);
        return params;
    }

    @Test
    public void 저장한_정의로_요청을_받아_워크플로우를_실행한다() throws Exception {
        RestApi saved = restApiService.save(saveParams(
                restApi("GOAL_SAVE", "POST", "goals/{evalId}/", "OBJECT", "ME_EVAL"),
                List.of(
                        parameter("PATH", "p1", null, "evalId", "INTEGER", true, "ME_GOALS"),
                        parameter("HEADER", "p2", null, "X-User-Id", "STRING", true, "ME_EVAL"),
                        parameter("BODY", "b1", null, "goals", "ARRAY", true, "ME_GOALS"),
                        parameter("BODY", "b2", "b1", "goalId", "INTEGER", true, null)
                )
        ));

        // 경로는 / 로 시작하고 끝의 / 는 떼어서 저장한다.
        assertEquals("/goals/{evalId}", saved.getPath());
        assertEquals(4, restApiParameterRepository.findByRestApiIdOrderByOrderNum(saved.getId()).size());

        WorkflowService workflowService = mock(WorkflowService.class);
        when(workflowService.execute(any(), any(), any())).thenReturn(ResponseMessage.createSuccessMessage(Map.of()));

        RestApiExecuteService executeService = executeService(workflowService);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/rest/goals/42");
        request.addHeader("x-user-id", "U01");

        RestApiResult.Json response = json(executeService.execute(request, new MockHttpServletResponse(),
                "{\"goals\":[{\"goalId\":1},{\"goalId\":2}]}"));

        assertEquals(200, response.status());

        verify(workflowService).execute(any(), any(), argThat((RequestMessage message) -> {
            assertEquals("WF_GOAL_SAVE", message.getHeader().getWorkflowCode());
            assertEquals(List.of(Map.of("X-User-Id", "U01")), message.getBody().get("ME_EVAL"));
            assertEquals(List.of(Map.of("goalId", 1, "evalId", 42), Map.of("goalId", 2, "evalId", 42)), message.getBody().get("ME_GOALS"));
            return true;
        }));

        // 경로는 맞는데 메소드가 다르면 405, 경로가 없으면 404, 스키마에 어긋나면 400, JSON 이 깨지면 400.
        assertEquals(405, json(executeService.execute(new MockHttpServletRequest("GET", "/rest/goals/42"), null, null)).status());
        assertEquals(404, json(executeService.execute(new MockHttpServletRequest("POST", "/rest/nothing"), null, "{}")).status());

        MockHttpServletRequest invalid = new MockHttpServletRequest("POST", "/rest/goals/abc");
        invalid.addHeader("X-User-Id", "U01");
        ResponseMessage invalidResponse = errorBody(executeService.execute(invalid, null, "{\"goals\":[]}"));
        assertEquals(400, invalidResponse.getStatus());
        assertEquals("Path Variable 'evalId' 는 INTEGER 타입이어야 합니다.", invalidResponse.getMessage());

        MockHttpServletRequest broken = new MockHttpServletRequest("POST", "/rest/goals/1");
        broken.addHeader("X-User-Id", "U01");
        ResponseMessage brokenResponse = errorBody(executeService.execute(broken, null, "{goals:"));
        assertEquals(400, brokenResponse.getStatus());
        assertEquals(RestApiErrorMessage.INVALID_REQUEST_BODY.getCode(), brokenResponse.getCode());
    }

    private RestApiExecuteService executeService(WorkflowService workflowService) {
        return new RestApiExecuteService(restApiRepository, restApiParameterRepository, restApiRequestMapper,
                new RestApiResponseMapper(), workflowService, new ObjectMapper());
    }

    private static RestApiResult.Json json(RestApiResult result) {
        return assertInstanceOf(RestApiResult.Json.class, result);
    }

    private static ResponseMessage errorBody(RestApiResult result) {
        return assertInstanceOf(ResponseMessage.class, json(result).body());
    }

    private static Map<String, List<Map<String, Object>>> contents(Object... messageIdAndRows) {
        Map<String, List<Map<String, Object>>> contents = new LinkedHashMap<>();
        for(int i = 0; i < messageIdAndRows.length; i += 2) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) messageIdAndRows[i + 1];
            contents.put((String) messageIdAndRows[i], rows);
        }
        return contents;
    }

    private Map<String, Object> responseItem(String paramKey, String parentKey, String name, String dataType, String mappingKey) {
        Map<String, Object> item = parameter("RESPONSE", paramKey, parentKey, name, dataType, false, null);
        item.put("mappingKey", mappingKey);
        return item;
    }

    private Map<String, Object> jsonApi(String apiCode, String path, String responseMessageId, String responseDataType) {
        Map<String, Object> api = restApi(apiCode, "GET", path, null, null);
        api.put("responseType", "JSON");
        api.put("responseMessageId", responseMessageId);
        api.put("responseDataType", responseDataType);
        return api;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void 응답메시지ID를_지정하면_그_메시지_내용만_메시지ID_키_없이_돌려준다() throws Exception {
        // 목록: ME_GOAL_LIST 의 행마다 goal_id -> goalId(INTEGER), score(NUMBER) 만.
        restApiService.save(saveParams(jsonApi("GOAL_LIST", "/goals", "ME_GOAL_LIST", "ARRAY"), List.of(
                responseItem("r1", null, "goalId", "INTEGER", "goal_id"),
                responseItem("r2", null, "score", "NUMBER", "")
        )));
        // 한 건: ME_GOAL_LIST 의 첫 행을 스키마 없이 그대로.
        restApiService.save(saveParams(jsonApi("GOAL_ONE", "/goals/first", "ME_GOAL_LIST", "OBJECT"), List.of()));
        // 워크플로우가 만들지 않은 메시지는 목록이면 [], 한 건이면 null.
        restApiService.save(saveParams(jsonApi("EMPTY_LIST", "/empty/list", "ME_NOT_EXECUTED", "ARRAY"), List.of()));
        restApiService.save(saveParams(jsonApi("EMPTY_ONE", "/empty/one", "ME_NOT_EXECUTED", "OBJECT"), List.of()));

        WorkflowService workflowService = mock(WorkflowService.class);
        when(workflowService.execute(any(), any(), any())).thenReturn(ResponseMessage.createSuccessMessage(contents(
                "ME_GOAL_LIST", List.of(Map.of("goal_id", "1", "score", "87.5", "secret", "x"), Map.of("goal_id", 2L)),
                "ME_HIDDEN", List.of(Map.of("a", 1))
        )));
        RestApiExecuteService executeService = executeService(workflowService);

        RestApiResult.Json response = json(executeService.execute(new MockHttpServletRequest("GET", "/rest/goals"), null, null));
        assertEquals(200, response.status());

        Map<String, Object> body = (Map<String, Object>) response.body();
        assertEquals(List.of("contents", "status", "message", "code", "resultType"), new ArrayList<>(body.keySet()));
        assertEquals("SUCCESS", String.valueOf(body.get("resultType")));

        List<Map<String, Object>> goals = (List<Map<String, Object>>) body.get("contents");
        assertEquals(2, goals.size());
        assertEquals(List.of("goalId", "score"), new ArrayList<>(goals.get(0).keySet()));
        assertEquals(1, goals.get(0).get("goalId"));
        assertEquals(new java.math.BigDecimal("87.5"), goals.get(0).get("score"));
        assertEquals(2L, goals.get(1).get("goalId"));
        assertNull(goals.get(1).get("score"));

        Map<String, Object> one = (Map<String, Object>) json(executeService.execute(new MockHttpServletRequest("GET", "/rest/goals/first"), null, null)).body();
        assertEquals(Map.of("goal_id", "1", "score", "87.5", "secret", "x"), one.get("contents"));

        Map<String, Object> emptyList = (Map<String, Object>) json(executeService.execute(new MockHttpServletRequest("GET", "/rest/empty/list"), null, null)).body();
        assertEquals(List.of(), emptyList.get("contents"));

        Map<String, Object> emptyOne = (Map<String, Object>) json(executeService.execute(new MockHttpServletRequest("GET", "/rest/empty/one"), null, null)).body();
        assertTrue(emptyOne.containsKey("contents"));
        assertNull(emptyOne.get("contents"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void 응답메시지를_설정하지_않으면_전체를_그대로_돌려주고_스키마에_어긋나면_500() throws Exception {
        restApiService.save(saveParams(restApi("RAW", "GET", "/raw", null, null), List.of()));
        restApiService.save(saveParams(jsonApi("TYPED", "/typed", "ME", "ARRAY"), List.of(
                responseItem("r1", null, "count", "INTEGER", "")
        )));

        Map<String, List<Map<String, Object>>> workflowContents = contents(
                "ME", List.of(Map.of("count", "많음")),
                "ME2", List.of(Map.of("b", 2))
        );

        WorkflowService workflowService = mock(WorkflowService.class);
        when(workflowService.execute(any(), any(), any())).thenReturn(ResponseMessage.createSuccessMessage(workflowContents));
        RestApiExecuteService executeService = executeService(workflowService);

        Map<String, Object> raw = (Map<String, Object>) json(executeService.execute(new MockHttpServletRequest("GET", "/rest/raw"), null, null)).body();
        assertEquals(workflowContents, raw.get("contents"));

        ResponseMessage typed = errorBody(executeService.execute(new MockHttpServletRequest("GET", "/rest/typed"), null, null));
        assertEquals(500, typed.getStatus());
        assertEquals(RestApiValidationException.RESPONSE_CODE, typed.getCode());
        assertEquals("Response 'contents[0].count' 는 INTEGER 타입이어야 합니다.", typed.getMessage());

        // 워크플로우가 실패하면 응답 설정과 상관없이 워크플로우 오류를 그대로 돌려준다.
        when(workflowService.execute(any(), any(), any())).thenReturn(ResponseMessage.createErrorMessage(422, "E-BIZ", "업무 오류"));
        ResponseMessage failed = errorBody(executeService.execute(new MockHttpServletRequest("GET", "/rest/typed"), null, null));
        assertEquals(422, failed.getStatus());
        assertEquals("E-BIZ", failed.getCode());
    }

    @Test
    public void 파일_다운로드_응답은_메시지의_파일_데이터를_내려준다() throws Exception {
        Map<String, Object> base64Api = restApi("FILE_BASE64", "GET", "/files/base64", null, null);
        base64Api.put("responseType", "FILE");
        base64Api.put("fileMessageId", "ME_FILE");
        base64Api.put("fileContentKey", "content");
        base64Api.put("fileContentEncoding", "BASE64");
        base64Api.put("fileNameKey", "file_name");
        base64Api.put("fileName", "default.bin");
        base64Api.put("fileContentTypeKey", "content_type");
        restApiService.save(saveParams(base64Api, List.of()));

        Map<String, Object> textApi = restApi("FILE_TEXT", "GET", "/files/text", null, null);
        textApi.put("responseType", "FILE");
        textApi.put("fileMessageId", "ME_FILE");
        textApi.put("fileContentKey", "text");
        textApi.put("fileContentEncoding", "TEXT");
        textApi.put("fileName", "보고서.csv");
        restApiService.save(saveParams(textApi, List.of()));

        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        Map<String, Object> row = new HashMap<>();
        row.put("content", "data:image/png;base64," + Base64.getEncoder().encodeToString(png));
        row.put("file_name", "logo.png");
        row.put("content_type", "");
        row.put("text", "a,b\n1,2");

        WorkflowService workflowService = mock(WorkflowService.class);
        when(workflowService.execute(any(), any(), any())).thenReturn(ResponseMessage.createSuccessMessage(contents("ME_FILE", List.of(row))));
        RestApiExecuteService executeService = executeService(workflowService);

        RestApiResponseMapper.FileContent base64 = assertInstanceOf(RestApiResult.File.class,
                executeService.execute(new MockHttpServletRequest("GET", "/rest/files/base64"), null, null)).content();
        assertArrayEquals(png, base64.bytes());
        assertEquals("logo.png", base64.fileName());
        // Content-Type 컬럼이 비어있으면 파일명 확장자로 정한다.
        assertEquals("image/png", base64.contentType().toString());

        RestApiResponseMapper.FileContent text = assertInstanceOf(RestApiResult.File.class,
                executeService.execute(new MockHttpServletRequest("GET", "/rest/files/text"), null, null)).content();
        assertEquals("a,b\n1,2", new String(text.bytes(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("보고서.csv", text.fileName());

        // 파일 메시지가 비어있으면 404, 내용이 Base64 가 아니면 500.
        when(workflowService.execute(any(), any(), any())).thenReturn(ResponseMessage.createSuccessMessage(contents("ME_FILE", List.of())));
        assertEquals(RestApiErrorMessage.NOT_FOUND_FILE.getCode(),
                errorBody(executeService.execute(new MockHttpServletRequest("GET", "/rest/files/base64"), null, null)).getCode());

        row.put("content", "!!!not base64!!!");
        when(workflowService.execute(any(), any(), any())).thenReturn(ResponseMessage.createSuccessMessage(contents("ME_FILE", List.of(row))));
        assertEquals(RestApiErrorMessage.INVALID_FILE_CONTENT.getCode(),
                errorBody(executeService.execute(new MockHttpServletRequest("GET", "/rest/files/base64"), null, null)).getCode());
    }

    @Test
    public void 고정_경로가_경로변수보다_먼저_맞는다() throws Exception {
        restApiService.save(saveParams(restApi("GOAL_ONE", "GET", "/goals/{goalId}", null, null),
                List.of(parameter("PATH", "p1", null, "goalId", "STRING", true, "ME"))));
        restApiService.save(saveParams(restApi("GOAL_SUMMARY", "GET", "/goals/summary", null, null), List.of()));

        RestApiExecuteService executeService = executeService(mock(WorkflowService.class));

        assertEquals("GOAL_SUMMARY", executeService.findApi(HttpMethodType.GET, "/goals/summary").restApi().getApiCode());
        assertEquals("GOAL_ONE", executeService.findApi(HttpMethodType.GET, "/goals/7").restApi().getApiCode());
        assertEquals(Map.of("goalId", "7"), executeService.findApi(HttpMethodType.GET, "/goals/7").pathVariables());
    }

    @Test
    public void 잘못된_정의는_저장하지_않는다() throws Exception {
        restApiService.save(saveParams(restApi("DUP", "GET", "/items/{id}", null, null),
                List.of(parameter("PATH", "p1", null, "id", "STRING", true, "ME"))));

        assertDefinitionError("이미 사용중인 API 코드입니다. (DUP)",
                saveParams(restApi("DUP", "GET", "/other", null, null), List.of()));

        // 경로변수 이름만 다른 경로는 같은 요청을 받으므로 막는다.
        assertDefinitionError("같은 메소드와 경로의 API 가 이미 있습니다. (DUP : GET /items/{id})",
                saveParams(restApi("DUP2", "GET", "/items/{itemId}", null, null),
                        List.of(parameter("PATH", "p1", null, "itemId", "STRING", true, "ME"))));

        assertDefinitionError("경로의 경로변수 [id] 와 정의한 Path Variable [] 가 일치하지 않습니다.",
                saveParams(restApi("NO_PATH_PARAM", "GET", "/users/{id}", null, null), List.of()));

        assertDefinitionError("GET 메소드는 Request Body 를 받을 수 없습니다.",
                saveParams(restApi("GET_BODY", "GET", "/get-body", "OBJECT", "ME"), List.of()));

        assertDefinitionError("Request Param 'page' 를 담을 요청메시지ID 를 입력해주세요.",
                saveParams(restApi("NO_MESSAGE", "GET", "/no-message", null, null),
                        List.of(parameter("QUERY", "p1", null, "page", "INTEGER", false, ""))));

        assertDefinitionError("Request Header 'X-A' 는 STRING, INTEGER, NUMBER, BOOLEAN 타입만 쓸 수 있습니다.",
                saveParams(restApi("HEADER_OBJECT", "GET", "/header-object", null, null),
                        List.of(parameter("HEADER", "p1", null, "X-A", "OBJECT", false, "ME"))));

        assertDefinitionError("Request Body 'id' 의 상위 필드(name)는 OBJECT/ARRAY 타입이어야 합니다.",
                saveParams(restApi("BAD_PARENT", "POST", "/bad-parent", "OBJECT", "ME"),
                        List.of(
                                parameter("BODY", "b1", null, "name", "STRING", false, null),
                                parameter("BODY", "b2", "b1", "id", "STRING", false, null)
                        )));

        Map<String, Object> badDefault = parameter("QUERY", "p1", null, "page", "INTEGER", false, "ME");
        badDefault.put("defaultValue", "abc");
        assertDefinitionError("Request Param 'page' 의 기본값 는 INTEGER 타입이어야 합니다.",
                saveParams(restApi("BAD_DEFAULT", "GET", "/bad-default", null, null), List.of(badDefault)));

        assertDefinitionError("응답스키마를 쓰려면 응답메시지ID 를 지정해주세요.",
                saveParams(jsonApi("NO_RESPONSE_MESSAGE", "/no-response-message", "", "ARRAY"),
                        List.of(responseItem("r1", null, "goalId", "INTEGER", "goal_id"))));

        assertDefinitionError("응답 형태는 ARRAY(목록) 또는 OBJECT(한 건) 여야 합니다.",
                saveParams(jsonApi("BAD_RESPONSE_TYPE", "/bad-response-type", "ME", "STRING"), List.of()));

        Map<String, Object> fileApi = restApi("FILE_NO_CONTENT", "GET", "/file-no-content", null, null);
        fileApi.put("responseType", "FILE");
        fileApi.put("fileMessageId", "ME_FILE");
        assertDefinitionError("파일 다운로드 응답의 파일내용 컬럼을 입력해주세요.", saveParams(fileApi, List.of()));

        fileApi.put("fileContentKey", "content");
        assertDefinitionError("파일 다운로드 응답에는 응답스키마를 설정할 수 없습니다.",
                saveParams(fileApi, List.of(responseItem("r1", null, "goalId", "INTEGER", "goal_id"))));
    }

    private void assertDefinitionError(String message, Map<String, Object> params) {
        RestApiValidationException e = assertThrows(RestApiValidationException.class, () -> restApiService.save(params));
        assertEquals(message, e.getMessage());
        assertEquals(RestApiValidationException.DEFINITION_CODE, e.getCode());
    }
}

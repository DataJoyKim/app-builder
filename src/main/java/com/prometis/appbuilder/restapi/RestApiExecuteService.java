package com.prometis.appbuilder.restapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.appbuilder.dto.ResponseMessage;
import com.prometis.appbuilder.dto.ResultType;
import com.prometis.appbuilder.restapi.code.HttpMethodType;
import com.prometis.appbuilder.restapi.code.ResponseType;
import com.prometis.appbuilder.workflow.WorkflowService;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPattern;

import java.util.*;

/**
 * URL_PREFIX/** 로 들어온 요청을 정의된 API 에 맞춰 워크플로우로 실행한다.
 * 1. 경로와 메소드로 API 를 찾는다. 경로는 맞는데 메소드가 다르면 405, 경로가 맞는 API 가 없으면 404.
 * 2. 파라미터 스키마대로 검증해서 요청메시지를 만든다. 스키마에 어긋나면 400.
 * 3. 워크플로우를 실행한다. 인증/권한은 워크플로우 설정을 따른다.
 * 4. 성공하면 응답 설정대로 돌려준다. JSON 은 응답메시지 선택/응답스키마를 적용하고, FILE 은 파일로 내려준다.
 *    워크플로우가 실패하면 응답유형과 상관없이 워크플로우의 오류 응답(JSON)을 그대로 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class RestApiExecuteService {
    private final RestApiRepository restApiRepository;
    private final RestApiParameterRepository restApiParameterRepository;
    private final RestApiRequestMapper restApiRequestMapper;
    private final RestApiResponseMapper restApiResponseMapper;
    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper;

    public RestApiResult execute(HttpServletRequest request, HttpServletResponse response, String rawBody) {
        try {
            HttpMethodType httpMethod = HttpMethodType.valueOf(request.getMethod().toUpperCase(Locale.ROOT));
            String apiPath = apiPathOf(request);

            MatchedApi matched = findApi(httpMethod, apiPath);
            RestApi restApi = matched.restApi();
            List<RestApiParameter> parameters = restApiParameterRepository.findByRestApiIdOrderByOrderNum(restApi.getId());

            RestApiRequestData requestData = RestApiRequestData.builder()
                    .pathVariables(matched.pathVariables())
                    .queryParameters(request.getParameterMap())
                    .headerReader(request::getHeader)
                    .body(restApi.hasBody() ? parseBody(rawBody) : null)
                    .build();

            Map<String, List<Map<String, Object>>> messages = restApiRequestMapper.map(restApi, parameters, requestData);

            ResponseMessage responseMessage = workflowService.execute(request, response, createRequestMessage(restApi.getWorkflowCode(), messages));

            if(!ResultType.SUCCESS.equals(responseMessage.getResultType())) {
                return new RestApiResult.Json(responseMessage.getStatus(), responseMessage);
            }

            if(ResponseType.FILE.equals(restApi.resolveResponseType())) {
                return new RestApiResult.File(restApiResponseMapper.extractFile(restApi, responseMessage.getContents()));
            }

            return new RestApiResult.Json(responseMessage.getStatus(), jsonBody(
                    restApiResponseMapper.shapeContents(restApi, parameters, responseMessage.getContents()),
                    responseMessage
            ));
        }
        catch (RestApiValidationException e) {
            return error(e.getStatus(), e.getCode(), e.getMessage());
        }
        catch (BusinessException e) {
            return error(e.getStatus(), e.getCode(), e.getMsg());
        }
    }

    private static RestApiResult error(int status, String code, String message) {
        return new RestApiResult.Json(status, ResponseMessage.createErrorMessage(status, code, message));
    }

    // 응답 모양(contents, status, message, code, resultType)은 워크플로우 응답과 같게 두고 contents 만 바꾼다.
    private static Map<String, Object> jsonBody(Object contents, ResponseMessage responseMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        body.put("status", responseMessage.getStatus());
        body.put("message", responseMessage.getMessage());
        body.put("code", responseMessage.getCode());
        body.put("resultType", responseMessage.getResultType());
        return body;
    }

    // 컨텍스트 경로와 URL_PREFIX 를 뗀 나머지. 아무것도 없으면 / 로 본다.
    static String apiPathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if(contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        String path = uri.startsWith(RestApi.URL_PREFIX) ? uri.substring(RestApi.URL_PREFIX.length()) : uri;

        return RestApiService.normalizePath(path.isEmpty() ? "/" : path);
    }

    MatchedApi findApi(HttpMethodType httpMethod, String apiPath) throws BusinessException {
        PathContainer pathContainer = PathContainer.parsePath(apiPath);

        List<MatchedApi> sameMethod = new ArrayList<>();
        boolean pathMatchedOtherMethod = false;

        for(RestApi restApi : restApiRepository.findAll()) {
            if(!restApi.isEnabled()) {
                continue;
            }

            PathPattern pattern = RestApiService.parsePattern(restApi.getPath());
            PathPattern.PathMatchInfo matchInfo = pattern.matchAndExtract(pathContainer);
            if(matchInfo == null) {
                continue;
            }

            if(httpMethod.equals(restApi.getHttpMethod())) {
                sameMethod.add(new MatchedApi(restApi, pattern, matchInfo.getUriVariables()));
            }
            else {
                pathMatchedOtherMethod = true;
            }
        }

        if(sameMethod.isEmpty()) {
            throw new BusinessException(pathMatchedOtherMethod
                    ? RestApiErrorMessage.METHOD_NOT_ALLOWED
                    : RestApiErrorMessage.NOT_FOUND_API);
        }

        // /goals/summary 와 /goals/{goalId} 가 둘 다 맞으면 고정 경로가 더 구체적이므로 그쪽을 쓴다.
        sameMethod.sort((a, b) -> PathPattern.SPECIFICITY_COMPARATOR.compare(a.pattern(), b.pattern()));

        return sameMethod.get(0);
    }

    private Object parseBody(String rawBody) throws BusinessException {
        if(rawBody == null || rawBody.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(rawBody, Object.class);
        }
        catch (JsonProcessingException e) {
            throw new BusinessException(RestApiErrorMessage.INVALID_REQUEST_BODY);
        }
    }

    private static RequestMessage createRequestMessage(String workflowCode, Map<String, List<Map<String, Object>>> messages) {
        RequestMessage.Header header = new RequestMessage.Header();
        header.setWorkflowCode(workflowCode);

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setHeader(header);
        // 워크플로우가 실행하면서 응답 슬롯을 이 맵에 채워 넣으므로 수정 가능한 맵이어야 한다.
        requestMessage.setBody(new HashMap<>(messages));

        return requestMessage;
    }

    record MatchedApi(RestApi restApi, PathPattern pattern, Map<String, String> pathVariables) {
    }
}

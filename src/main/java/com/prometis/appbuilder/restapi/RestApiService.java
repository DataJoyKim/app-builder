package com.prometis.appbuilder.restapi;

import com.prometis.appbuilder.restapi.code.FileContentEncoding;
import com.prometis.appbuilder.restapi.code.HttpMethodType;
import com.prometis.appbuilder.restapi.code.ParameterIn;
import com.prometis.appbuilder.restapi.code.ResponseType;
import com.prometis.appbuilder.restapi.code.SchemaDataType;
import com.prometis.appbuilder.util.DataTypeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 콘솔에서 REST API 정의를 저장/삭제한다. 파라미터는 노드/연결선처럼 저장할 때마다 통째로 갈아끼운다.
 */
@Service
@RequiredArgsConstructor
public class RestApiService {
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}/]+)}");

    private final RestApiRepository restApiRepository;
    private final RestApiParameterRepository restApiParameterRepository;

    @Transactional
    @SuppressWarnings("unchecked")
    public RestApi save(Map<String, Object> params) throws RestApiValidationException {
        Map<String, Object> restApiParams = (Map<String, Object>) params.get("restApi");
        List<Map<String, Object>> parameterParams = (List<Map<String, Object>>) params.get("parameters");

        if(restApiParams == null) {
            throw RestApiValidationException.definition("저장할 API 정보가 없습니다.");
        }

        Long id = parseId(restApiParams.get("id"));
        String apiCode = requireText(restApiParams, "apiCode", "API 코드");
        String displayName = requireText(restApiParams, "displayName", "API 명칭");
        HttpMethodType httpMethod = parseEnum(HttpMethodType.class, text(restApiParams, "httpMethod"), "HTTP 메소드", true);
        String path = normalizePath(requireText(restApiParams, "path", "경로"));
        String workflowCode = requireText(restApiParams, "workflowCode", "워크플로우");
        SchemaDataType bodyDataType = parseEnum(SchemaDataType.class, text(restApiParams, "bodyDataType"), "본문 타입", false);
        String bodyMessageId = text(restApiParams, "bodyMessageId");

        ResponseType responseType = parseEnum(ResponseType.class, text(restApiParams, "responseType"), "응답유형", false);
        if(responseType == null) {
            responseType = ResponseType.JSON;
        }

        boolean isFile = ResponseType.FILE.equals(responseType);
        String fileMessageId = isFile ? emptyToNullText(text(restApiParams, "fileMessageId")) : null;
        String fileContentKey = isFile ? emptyToNullText(text(restApiParams, "fileContentKey")) : null;
        FileContentEncoding fileContentEncoding = isFile
                ? parseEnum(FileContentEncoding.class, text(restApiParams, "fileContentEncoding"), "파일내용 인코딩", false)
                : null;

        // MULTIPART 는 업로드(요청)에서만 쓰는 인코딩이라 파일 다운로드 응답에는 쓸 수 없다.
        if(fileContentEncoding != null && fileContentEncoding.isRequestOnly()) {
            throw RestApiValidationException.definition("파일내용 인코딩 '" + fileContentEncoding + "' 는 응답에 사용할 수 없습니다.");
        }

        // JSON 응답 설정. 파일 다운로드면 쓰지 않는다.
        String responseMessageId = isFile ? null : emptyToNullText(text(restApiParams, "responseMessageId"));
        SchemaDataType responseDataType = isFile ? null
                : parseEnum(SchemaDataType.class, text(restApiParams, "responseDataType"), "응답 형태", false);
        if(responseDataType != null && !responseDataType.isContainer()) {
            throw RestApiValidationException.definition("응답 형태는 ARRAY(목록) 또는 OBJECT(한 건) 여야 합니다.");
        }
        if(responseMessageId != null && responseDataType == null) {
            responseDataType = SchemaDataType.ARRAY;
        }

        validatePath(path);
        validateBodyRoot(httpMethod, bodyDataType, bodyMessageId);
        validateUniqueness(id, apiCode, httpMethod, path);

        if(isFile) {
            if(fileMessageId == null) {
                throw RestApiValidationException.definition("파일 다운로드 응답의 파일 메시지ID 를 입력해주세요.");
            }
            if(fileContentKey == null) {
                throw RestApiValidationException.definition("파일 다운로드 응답의 파일내용 컬럼을 입력해주세요.");
            }
        }

        List<RestApiParameter> parameters = buildParameters(parameterParams);
        validateParameters(path, httpMethod, bodyDataType, parameters);
        validateResponseParameters(responseType, responseMessageId, parameters);

        RestApi restApi;
        if(id == null) {
            restApi = RestApi.builder()
                    .apiCode(apiCode)
                    .displayName(displayName)
                    .httpMethod(httpMethod)
                    .path(path)
                    .workflowCode(workflowCode)
                    .bodyDataType(bodyDataType)
                    .bodyMessageId(bodyDataType == null ? null : bodyMessageId)
                    .description(text(restApiParams, "description"))
                    .enabled(!Boolean.FALSE.equals(restApiParams.get("enabled")))
                    .build();
        }
        else {
            restApi = restApiRepository.findById(id)
                    .orElseThrow(() -> RestApiValidationException.definition("저장할 API 를 찾을 수 없습니다."));

            restApi.update(
                    apiCode,
                    displayName,
                    httpMethod,
                    path,
                    workflowCode,
                    bodyDataType,
                    bodyDataType == null ? null : bodyMessageId,
                    text(restApiParams, "description"),
                    !Boolean.FALSE.equals(restApiParams.get("enabled"))
            );
        }

        restApi.updateResponse(
                responseType,
                responseMessageId,
                responseMessageId == null ? null : responseDataType,
                fileMessageId,
                fileContentKey,
                fileContentEncoding == null && isFile ? FileContentEncoding.BASE64 : fileContentEncoding,
                isFile ? emptyToNullText(text(restApiParams, "fileNameKey")) : null,
                isFile ? emptyToNullText(text(restApiParams, "fileName")) : null,
                isFile ? emptyToNullText(text(restApiParams, "fileContentTypeKey")) : null
        );

        RestApi saved = restApiRepository.save(restApi);

        restApiParameterRepository.deleteByRestApiId(saved.getId());

        int orderNum = 0;
        for(RestApiParameter parameter : parameters) {
            restApiParameterRepository.save(RestApiParameter.builder()
                    .restApiId(saved.getId())
                    .paramIn(parameter.getParamIn())
                    .paramKey(parameter.getParamKey())
                    .parentKey(parameter.getParentKey())
                    .name(parameter.getName())
                    .dataType(parameter.getDataType())
                    .required(parameter.getRequired())
                    .defaultValue(parameter.getDefaultValue())
                    .messageId(parameter.getMessageId())
                    .mappingKey(parameter.getMappingKey())
                    .description(parameter.getDescription())
                    .orderNum(orderNum++)
                    .build());
        }

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        restApiParameterRepository.deleteByRestApiId(id);
        restApiRepository.deleteById(id);
    }

    // 앞뒤 공백을 지우고, / 로 시작하고, 끝의 / 는 뗀다. 같은 경로가 다른 모양으로 두 번 저장되지 않게 한다.
    static String normalizePath(String path) {
        String result = path.trim();

        if(!result.startsWith("/")) {
            result = "/" + result;
        }

        while(result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    static List<String> pathVariablesOf(String path) {
        List<String> names = new ArrayList<>();
        Matcher matcher = PATH_VARIABLE.matcher(path);

        while(matcher.find()) {
            names.add(matcher.group(1));
        }

        return names;
    }

    private static void validatePath(String path) throws RestApiValidationException {
        if(path.contains("?") || path.contains("#") || path.contains("*")) {
            throw RestApiValidationException.definition("경로에는 ?, #, * 를 쓸 수 없습니다. 쿼리 값은 Request Param 으로 정의해주세요.");
        }

        try {
            PathPatternParser.defaultInstance.parse(path);
        }
        catch (PatternParseException e) {
            throw RestApiValidationException.definition("경로 형식이 올바르지 않습니다. (" + path + ")");
        }

        List<String> variables = pathVariablesOf(path);
        if(new HashSet<>(variables).size() != variables.size()) {
            throw RestApiValidationException.definition("경로에 같은 이름의 경로변수가 두 번 이상 있습니다.");
        }
    }

    private static void validateBodyRoot(
            HttpMethodType httpMethod,
            SchemaDataType bodyDataType,
            String bodyMessageId
    ) throws RestApiValidationException {
        if(bodyDataType == null) {
            return;
        }

        if(!httpMethod.allowsBody()) {
            throw RestApiValidationException.definition("GET 메소드는 Request Body 를 받을 수 없습니다.");
        }

        if(!bodyDataType.isContainer()) {
            throw RestApiValidationException.definition("Request Body 최상위 타입은 OBJECT 또는 ARRAY 여야 합니다.");
        }

        if(isBlank(bodyMessageId)) {
            throw RestApiValidationException.definition("Request Body 를 담을 요청메시지ID 를 입력해주세요.");
        }
    }

    // 경로변수 이름만 다른 두 경로(/goals/{id}, /goals/{goalId})는 같은 요청을 받으므로 같은 경로로 본다.
    private void validateUniqueness(Long id, String apiCode, HttpMethodType httpMethod, String path) throws RestApiValidationException {
        Optional<RestApi> sameCode = restApiRepository.findByApiCode(apiCode);
        if(sameCode.isPresent() && !sameCode.get().getId().equals(id)) {
            throw RestApiValidationException.definition("이미 사용중인 API 코드입니다. (" + apiCode + ")");
        }

        String shape = pathShape(path);
        for(RestApi other : restApiRepository.findByHttpMethod(httpMethod)) {
            if(other.getId().equals(id)) {
                continue;
            }

            if(pathShape(other.getPath()).equals(shape)) {
                throw RestApiValidationException.definition(
                        "같은 메소드와 경로의 API 가 이미 있습니다. (" + other.getApiCode() + " : " + httpMethod + " " + other.getPath() + ")");
            }
        }
    }

    static String pathShape(String path) {
        return PATH_VARIABLE.matcher(path).replaceAll("{}").toLowerCase(Locale.ROOT);
    }

    private static List<RestApiParameter> buildParameters(List<Map<String, Object>> parameterParams) throws RestApiValidationException {
        List<RestApiParameter> parameters = new ArrayList<>();
        if(parameterParams == null) {
            return parameters;
        }

        int seq = 0;
        for(Map<String, Object> param : parameterParams) {
            seq++;

            ParameterIn paramIn = parseEnum(ParameterIn.class, text(param, "paramIn"), "파라미터 위치", true);
            String name = text(param, "name");
            if(isBlank(name)) {
                throw RestApiValidationException.definition(labelOf(paramIn) + " 의 이름을 입력해주세요.");
            }

            SchemaDataType dataType = parseEnum(SchemaDataType.class, text(param, "dataType"), "'" + name + "' 의 타입", true);
            String paramKey = text(param, "paramKey");

            parameters.add(RestApiParameter.builder()
                    .paramIn(paramIn)
                    .paramKey(isBlank(paramKey) ? "p" + seq : paramKey)
                    .parentKey(isTree(paramIn) ? emptyToNullText(text(param, "parentKey")) : null)
                    .name(name.trim())
                    .dataType(dataType)
                    // 경로변수는 경로의 한 자리라 비어서 들어올 수 없다. 항상 필수다. 응답 항목에는 필수 개념이 없다.
                    .required(ParameterIn.PATH.equals(paramIn)
                            || (!ParameterIn.RESPONSE.equals(paramIn) && Boolean.TRUE.equals(param.get("required"))))
                    .defaultValue(emptyToNullText(text(param, "defaultValue")))
                    // 응답 항목의 원본 메시지ID/컬럼은 mappingKey 에 둔다.
                    .messageId(ParameterIn.RESPONSE.equals(paramIn) ? null : emptyToNullText(text(param, "messageId")))
                    .mappingKey(emptyToNullText(text(param, "mappingKey")))
                    .description(emptyToNullText(text(param, "description")))
                    .build());
        }

        return parameters;
    }

    private static void validateParameters(
            String path,
            HttpMethodType httpMethod,
            SchemaDataType bodyDataType,
            List<RestApiParameter> parameters
    ) throws RestApiValidationException {
        Map<String, RestApiParameter> byKey = new HashMap<>();
        Set<String> scopeNames = new HashSet<>();
        Set<String> pathParamNames = new LinkedHashSet<>();

        for(RestApiParameter parameter : parameters) {
            if(byKey.put(parameter.getParamKey(), parameter) != null) {
                throw RestApiValidationException.definition("파라미터 식별값(paramKey)이 중복되었습니다. (" + parameter.getParamKey() + ")");
            }
        }

        for(RestApiParameter parameter : parameters) {
            String location = RestApiRequestMapper.locationOf(parameter);
            ParameterIn paramIn = parameter.getParamIn();

            // 헤더 이름은 대소문자를 가리지 않는다.
            String scopeName = paramIn + "|" + (parameter.getParentKey() == null ? "" : parameter.getParentKey()) + "|"
                    + (ParameterIn.HEADER.equals(paramIn) ? parameter.getName().toLowerCase(Locale.ROOT) : parameter.getName());
            if(!scopeNames.add(scopeName)) {
                throw RestApiValidationException.definition(location + " 가 중복 정의되었습니다.");
            }

            // 응답 항목은 validateResponseParameters 에서 따로 검증한다.
            if(ParameterIn.RESPONSE.equals(paramIn)) {
                continue;
            }

            if(ParameterIn.BODY.equals(paramIn)) {
                validateBodyParameter(httpMethod, bodyDataType, parameter, byKey, location);
            }
            else {
                if(parameter.getDataType().isContainer()) {
                    throw RestApiValidationException.definition(location + " 는 STRING, INTEGER, NUMBER, BOOLEAN 타입만 쓸 수 있습니다.");
                }

                if(!parameter.hasMessageId()) {
                    throw RestApiValidationException.definition(location + " 를 담을 요청메시지ID 를 입력해주세요.");
                }
            }

            if(ParameterIn.PATH.equals(paramIn)) {
                pathParamNames.add(parameter.getName());
            }

            if(parameter.getDefaultValue() != null) {
                if(parameter.getDataType().isContainer()) {
                    throw RestApiValidationException.definition(location + " 는 OBJECT/ARRAY 타입이라 기본값을 쓸 수 없습니다.");
                }

                try {
                    RestApiRequestMapper.convertScalar(parameter.getDataType(), parameter.getDefaultValue(), location + " 의 기본값");
                }
                catch (RestApiValidationException e) {
                    throw RestApiValidationException.definition(e.getMessage());
                }
            }
        }

        List<String> pathVariables = pathVariablesOf(path);
        if(!new HashSet<>(pathVariables).equals(pathParamNames)) {
            throw RestApiValidationException.definition(
                    "경로의 경로변수 " + pathVariables + " 와 정의한 Path Variable " + pathParamNames + " 가 일치하지 않습니다.");
        }
    }

    private static void validateBodyParameter(
            HttpMethodType httpMethod,
            SchemaDataType bodyDataType,
            RestApiParameter parameter,
            Map<String, RestApiParameter> byKey,
            String location
    ) throws RestApiValidationException {
        if(bodyDataType == null || !httpMethod.allowsBody()) {
            throw RestApiValidationException.definition("Request Body 를 받지 않는 API 에 본문 필드(" + parameter.getName() + ")가 정의되어 있습니다.");
        }

        if(parameter.hasMessageId() && !parameter.getDataType().isContainer()) {
            throw RestApiValidationException.definition(location + " 는 OBJECT/ARRAY 타입일 때만 별도 요청메시지ID 를 지정할 수 있습니다.");
        }

        // 부모를 따라 올라가며 부모가 실제로 있는 컨테이너인지, 순환하지 않는지 확인한다.
        Set<String> visited = new HashSet<>();
        RestApiParameter current = parameter;

        while(current.getParentKey() != null) {
            if(!visited.add(current.getParamKey())) {
                throw RestApiValidationException.definition(location + " 의 상위 필드 연결이 순환합니다.");
            }

            RestApiParameter parent = byKey.get(current.getParentKey());
            if(parent == null || !ParameterIn.BODY.equals(parent.getParamIn())) {
                throw RestApiValidationException.definition(location + " 의 상위 필드를 찾을 수 없습니다.");
            }

            if(!parent.getDataType().isContainer()) {
                throw RestApiValidationException.definition(location + " 의 상위 필드(" + parent.getName() + ")는 OBJECT/ARRAY 타입이어야 합니다.");
            }

            current = parent;
        }
    }

    private static String labelOf(ParameterIn paramIn) {
        return switch (paramIn) {
            case PATH -> "Path Variable";
            case QUERY -> "Request Param";
            case HEADER -> "Request Header";
            case BODY -> "Request Body 필드";
            case RESPONSE -> "Response 항목";
        };
    }

    private static boolean isTree(ParameterIn paramIn) {
        return ParameterIn.BODY.equals(paramIn) || ParameterIn.RESPONSE.equals(paramIn);
    }

    /**
     * 응답스키마 검증. 응답스키마는 지정한 응답메시지 행의 필드라서 응답메시지ID 가 있어야 쓸 수 있다.
     */
    private static void validateResponseParameters(
            ResponseType responseType,
            String responseMessageId,
            List<RestApiParameter> parameters
    ) throws RestApiValidationException {
        List<RestApiParameter> responseParameters = parameters.stream()
                .filter(p -> ParameterIn.RESPONSE.equals(p.getParamIn()))
                .toList();

        if(responseParameters.isEmpty()) {
            return;
        }

        if(ResponseType.FILE.equals(responseType)) {
            throw RestApiValidationException.definition("파일 다운로드 응답에는 응답스키마를 설정할 수 없습니다.");
        }

        if(responseMessageId == null) {
            throw RestApiValidationException.definition("응답스키마를 쓰려면 응답메시지ID 를 지정해주세요.");
        }

        Map<String, RestApiParameter> byKey = new HashMap<>();
        for(RestApiParameter parameter : responseParameters) {
            byKey.put(parameter.getParamKey(), parameter);
        }

        for(RestApiParameter parameter : responseParameters) {
            String location = RestApiRequestMapper.locationOf(parameter);

            Set<String> visited = new HashSet<>();
            RestApiParameter current = parameter;

            while(current.getParentKey() != null) {
                if(!visited.add(current.getParamKey())) {
                    throw RestApiValidationException.definition(location + " 의 상위 항목 연결이 순환합니다.");
                }

                RestApiParameter parent = byKey.get(current.getParentKey());
                if(parent == null) {
                    throw RestApiValidationException.definition(location + " 의 상위 항목을 찾을 수 없습니다.");
                }

                if(!parent.getDataType().isContainer()) {
                    throw RestApiValidationException.definition(location + " 의 상위 항목(" + parent.getName() + ")은 OBJECT/ARRAY 타입이어야 합니다.");
                }

                current = parent;
            }

            if(parameter.getDefaultValue() != null) {
                if(parameter.getDataType().isContainer()) {
                    throw RestApiValidationException.definition(location + " 는 OBJECT/ARRAY 타입이라 기본값을 쓸 수 없습니다.");
                }

                try {
                    RestApiRequestMapper.convertScalar(parameter.getDataType(), parameter.getDefaultValue(), location + " 의 기본값");
                }
                catch (RestApiValidationException e) {
                    throw RestApiValidationException.definition(e.getMessage());
                }
            }
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String label, boolean required) throws RestApiValidationException {
        if(isBlank(value)) {
            if(required) {
                throw RestApiValidationException.definition(label + " 를 선택해주세요.");
            }
            return null;
        }

        try {
            return Enum.valueOf(type, value.trim());
        }
        catch (IllegalArgumentException e) {
            throw RestApiValidationException.definition(label + " 값이 올바르지 않습니다. (" + value + ")");
        }
    }

    private static String requireText(Map<String, Object> params, String key, String label) throws RestApiValidationException {
        String value = text(params, key);
        if(isBlank(value)) {
            throw RestApiValidationException.definition(label + " 을(를) 입력해주세요.");
        }
        return value.trim();
    }

    private static String text(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    // 화면의 hidden input 에서 오는 id 는 문자열이다.
    private static Long parseId(Object value) throws RestApiValidationException {
        if(value == null || (value instanceof String s && s.isBlank())) {
            return null;
        }

        Long id = DataTypeUtil.valueLongOf(value);
        if(id != null) {
            return id;
        }

        try {
            return Long.valueOf(String.valueOf(value).trim());
        }
        catch (NumberFormatException e) {
            throw RestApiValidationException.definition("API id 값이 올바르지 않습니다. (" + value + ")");
        }
    }

    private static String emptyToNullText(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static PathPattern parsePattern(String path) {
        return PathPatternParser.defaultInstance.parse(path);
    }
}

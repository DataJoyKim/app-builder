package com.prometis.appbuilder.restapi;

import com.prometis.appbuilder.restapi.code.ParameterIn;
import com.prometis.appbuilder.restapi.code.SchemaDataType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 요청값을 파라미터 스키마로 검증/변환하고, 워크플로우 요청메시지(메시지ID -> 행 목록)로 옮겨 담는다.
 *
 * 담기는 규칙
 * - PATH / QUERY / HEADER 값은 각자 적어둔 요청메시지의 "기본 행" 하나에 mappingKey 로 담긴다.
 * - 본문 최상위가 OBJECT 면 bodyMessageId 의 기본 행에, ARRAY 면 원소마다 bodyMessageId 의 행 하나로 담긴다.
 * - 본문의 OBJECT/ARRAY 필드에 messageId 가 있으면 그 필드는 별도 요청메시지가 된다(OBJECT 는 기본 행, ARRAY 는 원소마다 한 행).
 *   messageId 가 없으면 상위 행에 중첩된 값(Map / List) 그대로 담긴다.
 * - 한 요청메시지에 기본 행 값과 배열 행이 같이 있으면, 기본 행 값을 배열의 모든 행에 채워 넣는다.
 *   (예: 경로변수 evalId + 본문 배열 goals 를 같은 메시지로 받으면 goals 의 행마다 evalId 가 들어간다)
 * - 객체(본문 최상위 포함)에 하위 필드를 하나라도 정의하면 정의한 필드만 넘긴다. 하위 필드를 정의하지 않은 객체는 들어온 그대로 넘긴다.
 * - 값이 없으면 기본값을 쓰고, 기본값도 없으면 필수 여부를 검사한 뒤 null 로 담는다.
 */
@Component
public class RestApiRequestMapper {

    public Map<String, List<Map<String, Object>>> map(
            RestApi restApi,
            List<RestApiParameter> parameters,
            RestApiRequestData request
    ) throws RestApiValidationException {
        MessageBuffer buffer = new MessageBuffer();
        List<RestApiParameter> ordered = sortByOrderNum(parameters);

        for(RestApiParameter parameter : ordered) {
            // BODY 는 아래에서 트리로 읽고, RESPONSE 는 응답에만 쓰인다.
            if(!parameter.getParamIn().isFlatRequest()) {
                continue;
            }

            String raw = request.valueOf(parameter.getParamIn(), parameter.getName());
            Object value = resolveScalar(parameter, (raw == null || raw.isEmpty()) ? null : raw, locationOf(parameter));

            buffer.base(parameter.getMessageId()).put(parameter.resolveMappingKey(), value);
        }

        if(restApi.hasBody()) {
            mapBody(restApi, childrenOf(ordered, null), request.getBody(), buffer, ordered);
        }

        return buffer.build();
    }

    private void mapBody(
            RestApi restApi,
            List<RestApiParameter> rootFields,
            Object body,
            MessageBuffer buffer,
            List<RestApiParameter> all
    ) throws RestApiValidationException {
        String messageId = restApi.getBodyMessageId();

        if(SchemaDataType.ARRAY.equals(restApi.getBodyDataType())) {
            // 본문이 없으면 빈 배열로 본다. 원소가 없으니 검증할 필드도 없다.
            if(body == null) {
                buffer.rows(messageId);
                return;
            }

            if(!(body instanceof List<?> list)) {
                throw RestApiValidationException.request("Request Body 는 배열(ARRAY)이어야 합니다.");
            }

            for(int i = 0; i < list.size(); i++) {
                String location = "body[" + i + "]";
                Object element = list.get(i);

                if(!(element instanceof Map<?, ?> elementMap)) {
                    throw RestApiValidationException.request("Request Body '" + location + "' 는 객체(OBJECT)여야 합니다.");
                }

                Map<String, Object> row = new LinkedHashMap<>();
                fillObject(rootFields, asStringMap(elementMap), row, location, buffer, all);
                buffer.rows(messageId).add(row);
            }
            return;
        }

        // 본문이 없으면 빈 객체로 보고 필수 필드 검증은 그대로 한다.
        if(body != null && !(body instanceof Map<?, ?>)) {
            throw RestApiValidationException.request("Request Body 는 객체(OBJECT)여야 합니다.");
        }

        Map<String, Object> source = body == null ? Map.of() : asStringMap((Map<?, ?>) body);
        fillObject(rootFields, source, buffer.base(messageId), "body", buffer, all);
    }

    // fields 정의대로 source 에서 값을 꺼내 row 에 담는다.
    private void fillObject(
            List<RestApiParameter> fields,
            Map<String, Object> source,
            Map<String, Object> row,
            String location,
            MessageBuffer buffer,
            List<RestApiParameter> all
    ) throws RestApiValidationException {
        // 하위 필드 정의가 없는 객체는 들어온 그대로 넘긴다.
        if(fields.isEmpty()) {
            row.putAll(source);
            return;
        }

        for(RestApiParameter field : fields) {
            String fieldLocation = location + "." + field.getName();
            Object raw = source.get(field.getName());
            List<RestApiParameter> children = childrenOf(all, field.getParamKey());

            switch (field.getDataType()) {
                case OBJECT -> fillObjectField(field, raw, row, fieldLocation, children, buffer, all);
                case ARRAY -> fillArrayField(field, raw, row, fieldLocation, children, buffer, all);
                default -> row.put(field.resolveMappingKey(), resolveScalar(field, raw, "Request Body '" + fieldLocation + "'"));
            }
        }
    }

    private void fillObjectField(
            RestApiParameter field,
            Object raw,
            Map<String, Object> row,
            String location,
            List<RestApiParameter> children,
            MessageBuffer buffer,
            List<RestApiParameter> all
    ) throws RestApiValidationException {
        if(raw == null) {
            requireIfNeeded(field, "Request Body '" + location + "'");
            return;
        }

        if(!(raw instanceof Map<?, ?> rawMap)) {
            throw RestApiValidationException.request("Request Body '" + location + "' 는 객체(OBJECT)여야 합니다.");
        }

        Map<String, Object> target = field.hasMessageId() ? buffer.base(field.getMessageId()) : new LinkedHashMap<>();
        fillObject(children, asStringMap(rawMap), target, location, buffer, all);

        if(!field.hasMessageId()) {
            row.put(field.resolveMappingKey(), target);
        }
    }

    private void fillArrayField(
            RestApiParameter field,
            Object raw,
            Map<String, Object> row,
            String location,
            List<RestApiParameter> children,
            MessageBuffer buffer,
            List<RestApiParameter> all
    ) throws RestApiValidationException {
        if(raw == null) {
            requireIfNeeded(field, "Request Body '" + location + "'");
            return;
        }

        if(!(raw instanceof List<?> list)) {
            throw RestApiValidationException.request("Request Body '" + location + "' 는 배열(ARRAY)이어야 합니다.");
        }

        List<Object> nested = new ArrayList<>();

        for(int i = 0; i < list.size(); i++) {
            String elementLocation = location + "[" + i + "]";
            Object element = list.get(i);
            Object value;

            if(children.isEmpty()) {
                value = element;
            }
            else {
                if(!(element instanceof Map<?, ?> elementMap)) {
                    throw RestApiValidationException.request("Request Body '" + elementLocation + "' 는 객체(OBJECT)여야 합니다.");
                }

                Map<String, Object> elementRow = new LinkedHashMap<>();
                fillObject(children, asStringMap(elementMap), elementRow, elementLocation, buffer, all);
                value = elementRow;
            }

            if(field.hasMessageId()) {
                // 원소가 기본값(문자열/숫자 등)이면 필드의 키로 한 행을 만든다.
                buffer.rows(field.getMessageId()).add(value instanceof Map<?, ?> valueMap
                        ? asStringMap(valueMap)
                        : singleValueRow(field.resolveMappingKey(), value));
            }
            else {
                nested.add(value);
            }
        }

        if(field.hasMessageId()) {
            buffer.rows(field.getMessageId());
        }
        else {
            row.put(field.resolveMappingKey(), nested);
        }
    }

    /**
     * 기본 타입(STRING/INTEGER/NUMBER/BOOLEAN) 값 하나를 검증하고 변환한다. 값이 없으면 기본값 -> 필수 여부 순으로 처리한다.
     */
    Object resolveScalar(RestApiParameter parameter, Object raw, String location) throws RestApiValidationException {
        if(raw == null) {
            if(parameter.getDefaultValue() != null && !parameter.getDefaultValue().isEmpty()) {
                return convertScalar(parameter.getDataType(), parameter.getDefaultValue(), location);
            }

            requireIfNeeded(parameter, location);
            return null;
        }

        return convertScalar(parameter.getDataType(), raw, location);
    }

    static Object convertScalar(SchemaDataType dataType, Object raw, String location) throws RestApiValidationException {
        if(raw instanceof Map || raw instanceof Collection) {
            throw typeMismatch(location, dataType);
        }

        switch (dataType) {
            case STRING:
                return String.valueOf(raw);
            case INTEGER:
                if(raw instanceof Integer || raw instanceof Long || raw instanceof Short || raw instanceof Byte) {
                    return raw;
                }
                try {
                    BigDecimal decimal = new BigDecimal(String.valueOf(raw).trim());
                    long value = decimal.longValueExact();
                    return (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) ? (Object) (int) value : (Object) value;
                }
                catch (NumberFormatException | ArithmeticException e) {
                    throw typeMismatch(location, dataType);
                }
            case NUMBER:
                if(raw instanceof Number) {
                    return raw;
                }
                try {
                    return new BigDecimal(String.valueOf(raw).trim());
                }
                catch (NumberFormatException e) {
                    throw typeMismatch(location, dataType);
                }
            case BOOLEAN:
                if(raw instanceof Boolean) {
                    return raw;
                }
                // DB 의 tinyint(1) / number(1) 처럼 1, 0 으로 오는 값도 참/거짓으로 본다.
                if(raw instanceof Number number && (number.doubleValue() == 1 || number.doubleValue() == 0)) {
                    return number.doubleValue() == 1;
                }
                String text = String.valueOf(raw).trim();
                if("true".equalsIgnoreCase(text)) return true;
                if("false".equalsIgnoreCase(text)) return false;
                throw typeMismatch(location, dataType);
            default:
                throw typeMismatch(location, dataType);
        }
    }

    private static void requireIfNeeded(RestApiParameter parameter, String location) throws RestApiValidationException {
        if(parameter.isRequired()) {
            throw RestApiValidationException.request(location + " 는 필수값입니다.");
        }
    }

    private static RestApiValidationException typeMismatch(String location, SchemaDataType dataType) {
        return RestApiValidationException.request(location + " 는 " + dataType.name() + " 타입이어야 합니다.");
    }

    static String locationOf(RestApiParameter parameter) {
        String label = switch (parameter.getParamIn()) {
            case PATH -> "Path Variable";
            case QUERY -> "Request Param";
            case HEADER -> "Request Header";
            case BODY -> "Request Body";
            case RESPONSE -> "Response";
        };

        return label + " '" + parameter.getName() + "'";
    }

    static List<RestApiParameter> childrenOf(List<RestApiParameter> all, String parentKey) {
        return childrenOf(all, ParameterIn.BODY, parentKey);
    }

    // paramIn 트리(BODY/RESPONSE)에서 parentKey 의 바로 아래 항목들. parentKey 가 null 이면 최상위.
    static List<RestApiParameter> childrenOf(List<RestApiParameter> all, ParameterIn paramIn, String parentKey) {
        List<RestApiParameter> children = new ArrayList<>();

        for(RestApiParameter parameter : all) {
            if(!paramIn.equals(parameter.getParamIn())) {
                continue;
            }

            boolean isRoot = parameter.getParentKey() == null || parameter.getParentKey().isBlank();
            boolean matches = (parentKey == null)
                    ? isRoot
                    : parentKey.equals(parameter.getParentKey());

            if(matches) {
                children.add(parameter);
            }
        }

        return children;
    }

    private static List<RestApiParameter> sortByOrderNum(List<RestApiParameter> parameters) {
        List<RestApiParameter> ordered = new ArrayList<>(parameters == null ? List.of() : parameters);
        ordered.sort(Comparator.comparing(RestApiParameter::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));
        return ordered;
    }

    private static Map<String, Object> singleValueRow(String key, Object value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(key, value);
        return row;
    }

    private static Map<String, Object> asStringMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for(Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    /**
     * 요청메시지별로 기본 행과 배열 행을 모아뒀다가 한 번에 조립한다.
     */
    static class MessageBuffer {
        private final Map<String, Map<String, Object>> baseRows = new LinkedHashMap<>();
        private final Map<String, List<Map<String, Object>>> arrayRows = new LinkedHashMap<>();
        private final LinkedHashSet<String> messageIds = new LinkedHashSet<>();

        Map<String, Object> base(String messageId) {
            messageIds.add(messageId);
            return baseRows.computeIfAbsent(messageId, key -> new LinkedHashMap<>());
        }

        List<Map<String, Object>> rows(String messageId) {
            messageIds.add(messageId);
            return arrayRows.computeIfAbsent(messageId, key -> new ArrayList<>());
        }

        Map<String, List<Map<String, Object>>> build() {
            Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

            for(String messageId : messageIds) {
                Map<String, Object> base = baseRows.get(messageId);
                List<Map<String, Object>> rows = arrayRows.get(messageId);

                if(rows == null) {
                    result.put(messageId, new ArrayList<>(List.of(base)));
                    continue;
                }

                if(base != null) {
                    for(Map<String, Object> row : rows) {
                        for(Map.Entry<String, Object> entry : base.entrySet()) {
                            row.putIfAbsent(entry.getKey(), entry.getValue());
                        }
                    }
                }

                result.put(messageId, new ArrayList<>(rows));
            }

            return result;
        }
    }
}

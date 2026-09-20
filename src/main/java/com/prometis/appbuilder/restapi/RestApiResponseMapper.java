package com.prometis.appbuilder.restapi;

import com.prometis.appbuilder.restapi.code.ParameterIn;
import com.prometis.appbuilder.restapi.code.SchemaDataType;
import com.prometis.core.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

/**
 * 워크플로우 결과(메시지ID -> 행 목록)를 API 의 응답 설정대로 바꾼다.
 *
 * JSON 응답
 * - 응답메시지ID(RestApi.responseMessageId)를 지정하지 않으면 contents 를 그대로 돌려준다.
 * - 지정하면 그 메시지의 내용만 메시지ID 키 없이 돌려준다. 호출하는 쪽은 워크플로우의 메시지ID 를 알 필요가 없다.
 *   응답 형태가 ARRAY 면 행 목록 전체, OBJECT 면 첫 행 하나(없으면 null)다.
 * - 응답스키마(RESPONSE 파라미터)는 그 메시지 행의 필드다. 정의하면 정의한 필드만 그 타입으로 바꿔 돌려주고,
 *   정의하지 않으면 행을 그대로 돌려준다. name 이 응답 필드명, mappingKey(비우면 name)가 원본 컬럼이다.
 *
 * 파일 응답
 * - fileMessageId 메시지의 첫 행에서 파일내용/파일명/Content-Type 컬럼을 읽는다.
 */
@Component
public class RestApiResponseMapper {
    private static final String DEFAULT_FILE_NAME = "download";
    private static final String CONTENTS = "contents";

    public Object shapeContents(
            RestApi restApi,
            List<RestApiParameter> parameters,
            Map<String, List<Map<String, Object>>> contents
    ) throws RestApiValidationException {
        if(!restApi.hasResponseMessageId()) {
            return contents;
        }

        List<RestApiParameter> ordered = new ArrayList<>(parameters == null ? List.of() : parameters);
        ordered.sort(Comparator.comparing(RestApiParameter::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));

        List<RestApiParameter> fields = RestApiRequestMapper.childrenOf(ordered, ParameterIn.RESPONSE, null);
        List<Map<String, Object>> rows = contents == null ? null : contents.get(restApi.getResponseMessageId());

        if(SchemaDataType.OBJECT.equals(restApi.resolveResponseDataType())) {
            Map<String, Object> first = (rows == null || rows.isEmpty()) ? null : rows.get(0);
            return first == null ? null : shapeObject(fields, first, CONTENTS, ordered);
        }

        List<Object> shapedRows = new ArrayList<>();
        if(rows != null) {
            for(int i = 0; i < rows.size(); i++) {
                shapedRows.add(shapeObject(fields, rows.get(i), CONTENTS + "[" + i + "]", ordered));
            }
        }

        return shapedRows;
    }

    private Map<String, Object> shapeObject(
            List<RestApiParameter> fields,
            Map<?, ?> source,
            String location,
            List<RestApiParameter> all
    ) throws RestApiValidationException {
        Map<String, Object> result = new LinkedHashMap<>();

        if(fields.isEmpty()) {
            for(Map.Entry<?, ?> entry : source.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }

        for(RestApiParameter field : fields) {
            String fieldLocation = location + "." + field.getName();
            result.put(field.getName(), shapeValue(field, source.get(field.resolveMappingKey()), fieldLocation, all));
        }

        return result;
    }

    private Object shapeValue(
            RestApiParameter field,
            Object raw,
            String location,
            List<RestApiParameter> all
    ) throws RestApiValidationException {
        List<RestApiParameter> children = RestApiRequestMapper.childrenOf(all, ParameterIn.RESPONSE, field.getParamKey());

        if(raw == null) {
            boolean hasDefault = field.getDefaultValue() != null && !field.getDefaultValue().isEmpty();
            return (hasDefault && !field.getDataType().isContainer())
                    ? convert(field.getDataType(), field.getDefaultValue(), location)
                    : null;
        }

        switch (field.getDataType()) {
            case OBJECT -> {
                if(!(raw instanceof Map<?, ?> map)) {
                    throw RestApiValidationException.response("Response '" + location + "' 는 객체(OBJECT)여야 합니다.");
                }
                return shapeObject(children, map, location, all);
            }
            case ARRAY -> {
                if(!(raw instanceof List<?> list)) {
                    throw RestApiValidationException.response("Response '" + location + "' 는 배열(ARRAY)이어야 합니다.");
                }
                if(children.isEmpty()) {
                    return list;
                }

                List<Object> shaped = new ArrayList<>();
                for(int i = 0; i < list.size(); i++) {
                    if(!(list.get(i) instanceof Map<?, ?> element)) {
                        throw RestApiValidationException.response("Response '" + location + "[" + i + "]' 는 객체(OBJECT)여야 합니다.");
                    }
                    shaped.add(shapeObject(children, element, location + "[" + i + "]", all));
                }
                return shaped;
            }
            default -> {
                return convert(field.getDataType(), raw, location);
            }
        }
    }

    private static Object convert(SchemaDataType dataType, Object raw, String location) throws RestApiValidationException {
        try {
            return RestApiRequestMapper.convertScalar(dataType, raw, "Response '" + location + "'");
        }
        catch (RestApiValidationException e) {
            throw RestApiValidationException.response(e.getMessage());
        }
    }

    public FileContent extractFile(
            RestApi restApi,
            Map<String, List<Map<String, Object>>> contents
    ) throws BusinessException {
        List<Map<String, Object>> rows = contents == null ? null : contents.get(restApi.getFileMessageId());
        if(rows == null || rows.isEmpty() || rows.get(0) == null) {
            throw new BusinessException(RestApiErrorMessage.NOT_FOUND_FILE);
        }

        Map<String, Object> row = rows.get(0);
        Object rawContent = row.get(restApi.getFileContentKey());
        if(rawContent == null) {
            throw new BusinessException(RestApiErrorMessage.NOT_FOUND_FILE);
        }

        byte[] bytes = readBytes(rawContent, restApi);
        String fileName = resolveFileName(restApi, row);

        return new FileContent(bytes, fileName, resolveContentType(restApi, row, fileName));
    }

    private static byte[] readBytes(Object rawContent, RestApi restApi) throws BusinessException {
        if(rawContent instanceof byte[] bytes) {
            return bytes;
        }

        if(rawContent instanceof Blob blob) {
            try {
                return blob.getBytes(1, (int) blob.length());
            }
            catch (SQLException e) {
                throw new BusinessException(RestApiErrorMessage.INVALID_FILE_CONTENT);
            }
        }

        if(rawContent instanceof String text) {
            switch (restApi.resolveFileContentEncoding()) {
                case TEXT -> {
                    return text.getBytes(StandardCharsets.UTF_8);
                }
                // MULTIPART 는 업로드(요청)에서만 쓰는 인코딩이다.
                case MULTIPART -> throw new BusinessException(RestApiErrorMessage.INVALID_FILE_CONTENT);
                default -> {
                    try {
                        // data URL(data:image/png;base64,....) 로 온 값도 받는다.
                        int comma = text.startsWith("data:") ? text.indexOf(',') : -1;
                        return Base64.getMimeDecoder().decode(comma >= 0 ? text.substring(comma + 1) : text);
                    }
                    catch (IllegalArgumentException e) {
                        throw new BusinessException(RestApiErrorMessage.INVALID_FILE_CONTENT);
                    }
                }
            }
        }

        throw new BusinessException(RestApiErrorMessage.INVALID_FILE_CONTENT);
    }

    private static String resolveFileName(RestApi restApi, Map<String, Object> row) {
        Object fromRow = isBlank(restApi.getFileNameKey()) ? null : row.get(restApi.getFileNameKey());
        if(fromRow != null && !String.valueOf(fromRow).isBlank()) {
            return String.valueOf(fromRow).trim();
        }

        return isBlank(restApi.getFileName()) ? DEFAULT_FILE_NAME : restApi.getFileName().trim();
    }

    private static MediaType resolveContentType(RestApi restApi, Map<String, Object> row, String fileName) {
        Object fromRow = isBlank(restApi.getFileContentTypeKey()) ? null : row.get(restApi.getFileContentTypeKey());
        if(fromRow != null && !String.valueOf(fromRow).isBlank()) {
            try {
                return MediaType.parseMediaType(String.valueOf(fromRow).trim());
            }
            catch (IllegalArgumentException e) {
                // 잘못된 값이면 확장자로 정한다.
            }
        }

        return MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record FileContent(byte[] bytes, String fileName, MediaType contentType) {
    }
}

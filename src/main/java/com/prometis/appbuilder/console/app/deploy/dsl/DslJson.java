package com.prometis.appbuilder.console.app.deploy.dsl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DSL 파일의 JSON 읽기/쓰기.
 * 같은 메타데이터면 언제 어디서 만들어도 글자 하나까지 같아야 저장소와 비교했을 때 바뀐 파일만 잡힌다.
 * 그래서 필드 순서는 만드는 쪽(LinkedHashMap)이 정하고, 줄바꿈은 OS 와 상관없이 \n 으로 고정한다.
 */
public final class DslJson {
    public static final int VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter WRITER;

    static {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withSeparators(Separators.createDefaultInstance().withObjectFieldValueSpacing(Separators.Spacing.AFTER));
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);

        WRITER = MAPPER.writer(printer);
    }

    private DslJson() {
    }

    public static String write(Object value) {
        try {
            return WRITER.writeValueAsString(value) + "\n";
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static JsonNode read(String content) throws JsonProcessingException {
        return MAPPER.readTree(content);
    }

    /**
     * 문자열 컬럼에 JSON(객체/배열)이 들어있으면 풀어서 담는다. 한 줄짜리 JSON 문자열보다 비교(diff)가 읽기 쉽다.
     * JSON 이 아니면(HTML, 스크립트 등) 문자열 그대로 둔다.
     */
    public static Object jsonOrText(String value) {
        if(value == null || value.isBlank()) {
            return value;
        }

        String trimmed = value.trim();
        if(!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return value;
        }

        try {
            JsonNode node = MAPPER.readTree(trimmed);
            return (node != null && node.isContainerNode()) ? node : value;
        }
        catch (JsonProcessingException e) {
            return value;
        }
    }

    /**
     * 모든 DSL 파일 앞에 붙는 kind, version 으로 시작하는 객체.
     */
    public static Map<String, Object> document(DslFolder folder) {
        return document(folder.kind());
    }

    public static Map<String, Object> document(String kind) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("kind", kind);
        document.put("version", VERSION);
        return document;
    }
}

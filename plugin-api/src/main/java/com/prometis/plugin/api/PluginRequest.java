package com.prometis.plugin.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 플러그인으로 넘기는 요청 정보.
 *
 * <p>{@link #getContents()} 는 워크플로우 노드의 요청메시지(IN) 행 목록이다. 한 행이 하나의 Map 이다.
 * {@link #getAttributes()} 는 요청메시지 밖의 호출 정보(로그인 사용자 등)이며, 호스트가 채우는 키는 {@link #ATTR_USER_ID} 등 상수로 정의되어 있다.</p>
 */
public class PluginRequest {
    /** 로그인 사용자ID (Long). 인증 없이 실행된 워크플로우면 없다 */
    public static final String ATTR_USER_ID = "userId";
    /** 로그인 사용자명 (String). 인증 없이 실행된 워크플로우면 없다 */
    public static final String ATTR_USER_NAME = "userName";
    /** 실행 중인 워크플로우의 노드 기능코드 (= pluginPath) */
    public static final String ATTR_FUNCTION_NAME = "functionName";

    private final List<Map<String, Object>> contents;
    private final Map<String, Object> attributes;

    public PluginRequest(List<Map<String, Object>> contents) {
        this(contents, null);
    }

    public PluginRequest(List<Map<String, Object>> contents, Map<String, Object> attributes) {
        this.contents = (contents == null) ? new ArrayList<>() : contents;
        this.attributes = (attributes == null) ? new HashMap<>() : new HashMap<>(attributes);
    }

    /** 요청메시지 행 목록. 비어 있을 수 있다 */
    public List<Map<String, Object>> getContents() {
        return contents;
    }

    /** 첫 번째 요청메시지 행. 행이 없으면 빈 Map */
    public Map<String, Object> getFirstContent() {
        return contents.isEmpty() ? Collections.emptyMap() : contents.get(0);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}

package com.prometis.plugin.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 플러그인 실행 결과. {@link #getContents()} 의 행 목록이 워크플로우 노드의 응답메시지(OUT)가 된다.
 * 실패({@link #failure(String)})면 워크플로우는 그 노드를 실패로 처리한다.
 */
public class PluginResult {
    private final boolean success;
    private final String message;
    private final List<Map<String, Object>> contents;

    private PluginResult(boolean success, String message, List<Map<String, Object>> contents) {
        this.success = success;
        this.message = message;
        this.contents = (contents == null) ? new ArrayList<>() : contents;
    }

    public static PluginResult success(List<Map<String, Object>> contents) {
        return new PluginResult(true, null, contents);
    }

    public static PluginResult success(Map<String, Object> content) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if(content != null) {
            contents.add(content);
        }
        return new PluginResult(true, null, contents);
    }

    public static PluginResult failure(String message) {
        return new PluginResult(false, message, null);
    }

    public static PluginResult failure(String message, List<Map<String, Object>> contents) {
        return new PluginResult(false, message, contents);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<Map<String, Object>> getContents() {
        return contents;
    }
}

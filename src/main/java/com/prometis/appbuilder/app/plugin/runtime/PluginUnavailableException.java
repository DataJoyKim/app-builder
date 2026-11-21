package com.prometis.appbuilder.app.plugin.runtime;

/**
 * 실행하려는 플러그인이 이 서버에서 시작되어 있지 않거나 WorkflowNodePlugin 구현체가 없을 때.
 */
public class PluginUnavailableException extends RuntimeException {
    public PluginUnavailableException(String message) {
        super(message);
    }
}

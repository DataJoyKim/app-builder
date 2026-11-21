package com.prometis.appbuilder.app.plugin;

/**
 * 콘솔 화면에 그대로 보여줄 플러그인 관리 오류 (잘못된 jar, 개수 제한, 사용 중인 경로 삭제 등).
 */
public class PluginException extends RuntimeException {
    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}

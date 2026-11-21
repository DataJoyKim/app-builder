package com.prometis.appbuilder.app.plugin.runtime;

/**
 * 이 서버가 플러그인을 실제로 들고 있는 상태.
 *
 * @param revision 로드한(로드에 실패했다면 시도한) jar 의 revision. 아직 로드하지 않았으면 null
 * @param state    pf4j PluginState 이름(STARTED, STOPPED, RESOLVED, FAILED ...) 또는 {@link #NOT_LOADED}
 * @param message  실패 사유 등 콘솔에 보여줄 설명. 없으면 null
 */
public record LocalPluginState(Long revision, String state, String message) {
    public static final String NOT_LOADED = "NOT_LOADED";
    public static final String FAILED = "FAILED";

    public static LocalPluginState notLoaded() {
        return new LocalPluginState(null, NOT_LOADED, null);
    }

    public boolean isStarted() {
        return "STARTED".equals(state);
    }
}

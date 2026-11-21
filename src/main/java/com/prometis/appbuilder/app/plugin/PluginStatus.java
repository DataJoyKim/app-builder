package com.prometis.appbuilder.app.plugin;

import com.prometis.appbuilder.app.util.EnumStringConverter;

/**
 * 콘솔에서 지정한 플러그인의 목표 상태. 모든 서버가 이 상태로 맞춰진다 (PluginSynchronizer).
 */
public enum PluginStatus {
    STARTED, STOPPED;

    public static class Converter extends EnumStringConverter<PluginStatus> {
        public Converter() {
            super(PluginStatus.class);
        }
    }
}

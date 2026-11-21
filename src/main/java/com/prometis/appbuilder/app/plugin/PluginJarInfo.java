package com.prometis.appbuilder.app.plugin;

/**
 * 업로드한 플러그인 jar 의 MANIFEST 에서 읽은 정보.
 */
public record PluginJarInfo(String pluginId, String version, String provider, String description) {
}

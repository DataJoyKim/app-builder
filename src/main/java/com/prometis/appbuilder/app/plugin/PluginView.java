package com.prometis.appbuilder.app.plugin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 콘솔 플러그인 관리 화면에 내려주는 플러그인 정보.
 *
 * @param servers 서버(인스턴스)별 반영 상태. synced 가 false 인 서버는 아직 목표 상태로 맞춰지지 않은 것이다
 */
public record PluginView(
        Long id,
        String pluginId,
        String version,
        String provider,
        String description,
        String originalFilename,
        Long fileSize,
        String checksum,
        Long revision,
        PluginStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PathView> paths,
        List<ServerView> servers
) {
    public record PathView(Long id, String pluginId, String pluginPath, String displayName, String description, Integer orderNum) {
        public static PathView of(PluginPath path, String pluginId) {
            return new PathView(path.getId(), pluginId, path.getPluginPath(), path.getDisplayName(), path.getDescription(), path.getOrderNum());
        }
    }

    public record ServerView(String serverId, boolean currentServer, Long revision, String state, String message,
                             boolean synced, LocalDateTime updatedAt) {
    }

    /**
     * pluginPath 저장 요청 한 줄
     */
    public record PathRequest(String pluginPath, String displayName, String description) {
    }
}

package com.prometis.appbuilder.app.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * application.yml 의 platform.plugin 설정.
 *
 * @param rootPath     플러그인 외부 저장소 최상위 경로. 애플리케이션별 플러그인은 {rootPath}/{applicationId}/plugins 에 놓인다
 * @param maxCount     애플리케이션 하나에 등록할 수 있는 플러그인 최대 개수
 * @param maxFileSize  플러그인 jar 한 개의 최대 크기. spring.servlet.multipart 한도를 넘게 잡아도 그 한도에서 먼저 막힌다
 * @param syncInterval 다른 서버에서 바뀐 플러그인(DB)을 이 서버에 반영하는 주기. 0 이면 기동 시에만 맞춘다
 * @param serverId     콘솔에 서버별 상태를 보여줄 때 쓰는 이 서버의 이름. 비어 있으면 호스트명:포트
 */
@ConfigurationProperties(prefix = "platform.plugin")
public record PluginProperties(String rootPath, Integer maxCount, DataSize maxFileSize, Duration syncInterval, String serverId) {
    private static final String DEFAULT_ROOT_PATH = "C:/prometis";
    private static final int DEFAULT_MAX_COUNT = 10;
    private static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(30);
    private static final Duration DEFAULT_SYNC_INTERVAL = Duration.ofSeconds(30);

    public PluginProperties {
        if(rootPath == null || rootPath.isBlank()) {
            rootPath = DEFAULT_ROOT_PATH;
        }
        if(maxCount == null || maxCount < 0) {
            maxCount = DEFAULT_MAX_COUNT;
        }
        if(maxFileSize == null || maxFileSize.toBytes() <= 0) {
            maxFileSize = DEFAULT_MAX_FILE_SIZE;
        }
        if(syncInterval == null || syncInterval.isNegative()) {
            syncInterval = DEFAULT_SYNC_INTERVAL;
        }
    }
}

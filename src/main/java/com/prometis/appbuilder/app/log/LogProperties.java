package com.prometis.appbuilder.app.log;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 platform.log 설정. 워크플로우 실행 로그와 화면 접근 로그의 보관 기간.
 *
 * @param retentionDays 로그를 남겨 두는 일수. 오늘을 포함해 이 일수보다 오래된 날짜의 로그는 정리 작업이 지운다.
 *                      0 이하면 지우지 않는다. 콘솔 대시보드가 최근 30일까지 보여주므로 30 보다 작으면 그 기간이 비어 보인다
 */
@ConfigurationProperties(prefix = "platform.log")
public record LogProperties(Integer retentionDays) {
    public static final int DEFAULT_RETENTION_DAYS = 90;

    public LogProperties {
        if(retentionDays == null) {
            retentionDays = DEFAULT_RETENTION_DAYS;
        }
    }

    public boolean keepForever() {
        return retentionDays <= 0;
    }
}

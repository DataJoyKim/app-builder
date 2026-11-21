package com.prometis.appbuilder.app.view;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * application.yml 의 platform.view 설정.
 *
 * @param rootPath    화면 소스 외부 저장소 최상위 경로. FILE 유형 화면의 HTML 소스는 {rootPath}/{applicationId}/views/{objectCode}.html 에 놓인다
 * @param maxFileSize 화면 소스 파일 한 개의 최대 크기
 */
@ConfigurationProperties(prefix = "platform.view")
public record ViewProperties(String rootPath, DataSize maxFileSize) {
    private static final String DEFAULT_ROOT_PATH = "C:/prometis";
    private static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(2);

    public ViewProperties {
        if(rootPath == null || rootPath.isBlank()) {
            rootPath = DEFAULT_ROOT_PATH;
        }
        if(maxFileSize == null || maxFileSize.toBytes() <= 0) {
            maxFileSize = DEFAULT_MAX_FILE_SIZE;
        }
    }
}

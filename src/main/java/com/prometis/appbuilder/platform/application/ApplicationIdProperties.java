package com.prometis.appbuilder.platform.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

/**
 * application.yml의 platform.application.* 설정.
 *
 * @param reservedIds 애플리케이션ID로 쓸 수 없는 경로 (이 서버가 이미 쓰는 최상위 경로)
 */
@ConfigurationProperties(prefix = "platform.application")
public record ApplicationIdProperties(List<String> reservedIds) {
    public ApplicationIdProperties {
        // yml의 빈 항목("- ")은 null로 들어오므로 걸러낸다
        reservedIds = reservedIds == null
                ? List.of()
                : reservedIds.stream().filter(Objects::nonNull).toList();
    }
}

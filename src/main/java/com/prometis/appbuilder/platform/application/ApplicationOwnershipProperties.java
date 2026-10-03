package com.prometis.appbuilder.platform.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 platform.application.max-owned-count 설정.
 *
 * @param maxOwnedCount 한 사용자가 관리자(APPLICATION_ADMIN)로 소유할 수 있는 애플리케이션 최대 개수. 이 개수에 이르면 애플리케이션 콘솔에서 새로 만들 수 없다.
 */
@ConfigurationProperties(prefix = "platform.application")
public record ApplicationOwnershipProperties(Integer maxOwnedCount) {
    private static final int DEFAULT_MAX_OWNED_COUNT = 3;

    public ApplicationOwnershipProperties {
        if(maxOwnedCount == null || maxOwnedCount < 0) {
            maxOwnedCount = DEFAULT_MAX_OWNED_COUNT;
        }
    }
}

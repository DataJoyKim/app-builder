package com.prometis.appbuilder.platform.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * applicationId는 서비스 경로의 prefix로 쓰인다 (예: ehr → https://test.domain.com/ehr).
 * 그래서 URL 경로로 안전한 소문자만 허용하고, 이 서버가 이미 쓰는 최상위 경로와 겹치는 값은 막는다.
 * 예약 경로 목록은 application.yml의 platform.application.reserved-ids에서 읽는다.
 */
@Component
@EnableConfigurationProperties(ApplicationIdProperties.class)
public class ApplicationIdPolicy {
    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9_-]+$");

    private final Set<String> reservedIds;

    public ApplicationIdPolicy(ApplicationIdProperties properties) {
        // 설정에 "/console", " Console " 처럼 적어도 applicationId와 같은 형태로 비교되도록 정리
        Set<String> ids = new TreeSet<>();
        for(String id : properties.reservedIds()) {
            String normalized = id.trim().replaceFirst("^/+", "").toLowerCase(Locale.ROOT);
            if(!normalized.isEmpty()) ids.add(normalized);
        }
        this.reservedIds = Collections.unmodifiableSet(ids);
    }

    public Set<String> getReservedIds() {
        return reservedIds;
    }

    /** 규칙 위반이면 메시지를, 통과하면 null을 반환한다. */
    public String validate(String applicationId) {
        if(applicationId == null || applicationId.isBlank()) {
            return "애플리케이션ID를 입력해주세요.";
        }
        if(!PATTERN.matcher(applicationId).matches()) {
            return "애플리케이션ID는 영문 소문자, 숫자, '_', '-'만 사용할 수 있습니다.";
        }
        if(reservedIds.contains(applicationId)) {
            return "'" + applicationId + "'은(는) 시스템에서 사용하는 경로라 애플리케이션ID로 쓸 수 없습니다.";
        }
        return null;
    }
}

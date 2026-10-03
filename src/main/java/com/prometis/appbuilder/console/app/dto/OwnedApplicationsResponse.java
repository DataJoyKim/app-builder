package com.prometis.appbuilder.console.app.dto;

import com.prometis.appbuilder.platform.application.Application;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 로그인한 사용자가 관리자(APPLICATION_ADMIN)로 소유한 애플리케이션 목록과 생성 한도.
 */
@Getter
@Builder
public class OwnedApplicationsResponse {
    private List<Application> applications;
    private int maxOwnedCount;
    private Boolean creatable;
}

package com.prometis.appbuilder.console.app.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 콘솔 첫 화면(console/app/layout/content.html)의 애플리케이션 사용현황 카드.
 */
@RestController
@RequestMapping("/{applicationId}/console/api/dashboard")
@RequiredArgsConstructor
public class AppDashboardRestController {
    private final AppDashboardService appDashboardService;

    @GetMapping("")
    public ResponseEntity<AppDashboardResponse> dashboard(@PathVariable("applicationId") String applicationId) {
        return ResponseEntity.ok(appDashboardService.dashboard(applicationId));
    }
}

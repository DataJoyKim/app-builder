package com.prometis.appbuilder.console.app.dashboard;

import java.time.LocalDate;
import java.util.List;

/**
 * 콘솔 첫 화면의 "활동 추이". 실행/접근 로그(WorkflowExecutionLog, ViewAccessLog)를 from ~ to (오늘) 기간으로 센다.
 * daily 는 기간의 모든 날짜를 빠짐없이 담는다(기록이 없는 날은 0).
 */
public record AppActivityResponse(
        int days,
        LocalDate from,
        LocalDate to,
        Workflow workflow,
        View view
) {
    /**
     * @param total         기간 실행 수
     * @param success       그 중 성공
     * @param failure       그 중 실패 (워크플로우 오류, 인증/권한/IP/도메인 거부 포함)
     * @param today         오늘 실행 수
     * @param users         실행한 사용자 수 (익명 제외)
     * @param avgDurationMs 평균 처리시간(ms), 실행이 없으면 null
     */
    public record Workflow(long total, long success, long failure, long today, long users, Double avgDurationMs,
                           List<WorkflowDaily> daily, List<TopWorkflow> top) {
    }

    public record WorkflowDaily(LocalDate date, long success, long failure) {
    }

    public record TopWorkflow(String workflowCode, String displayName, long count, long failure) {
    }

    /**
     * @param total  기간 접근 수 (화면을 연 것만)
     * @param denied 로그인 필요/권한 없음으로 막힌 접근 수
     * @param today  오늘 접근 수
     * @param users  접근한 사용자 수 (익명 제외)
     */
    public record View(long total, long denied, long today, long users,
                       List<ViewDaily> daily, List<TopView> topViews, List<TopMenu> topMenus) {
    }

    public record ViewDaily(LocalDate date, long allowed, long denied) {
    }

    public record TopView(String objectCode, String objectName, long count) {
    }

    public record TopMenu(String menuCd, String menuNm, long count) {
    }
}

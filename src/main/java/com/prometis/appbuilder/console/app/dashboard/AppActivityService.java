package com.prometis.appbuilder.console.app.dashboard;

import com.prometis.appbuilder.app.view.log.ViewAccessLogRepository;
import com.prometis.appbuilder.app.view.log.ViewAccessResult;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 콘솔 첫 화면의 워크플로우 실행 추이 / 화면 접근 추이. 로그를 날짜별로 묶어 센다.
 */
@Service
@RequiredArgsConstructor
public class AppActivityService {
    static final Set<Integer> ALLOWED_DAYS = Set.of(7, 14, 30);
    static final int DEFAULT_DAYS = 14;
    static final int TOP_LIMIT = 5;

    private final WorkflowExecutionLogRepository workflowExecutionLogRepository;
    private final ViewAccessLogRepository viewAccessLogRepository;
    private final WorkflowRepository workflowRepository;

    @Transactional(readOnly = true)
    public AppActivityResponse activity(String applicationId, Integer days) {
        return activity(applicationId, days, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public AppActivityResponse activity(String applicationId, Integer days, LocalDate today) {
        int period = days != null && ALLOWED_DAYS.contains(days) ? days : DEFAULT_DAYS;
        LocalDate from = today.minusDays(period - 1L);

        return new AppActivityResponse(period, from, today,
                workflow(applicationId, from, today),
                view(applicationId, from, today));
    }

    private AppActivityResponse.Workflow workflow(String applicationId, LocalDate from, LocalDate today) {
        Map<LocalDate, long[]> byDate = emptyDays(from, today);
        for(Object[] row : workflowExecutionLogRepository.countDaily(applicationId, from)) {
            long[] counts = byDate.get((LocalDate) row[0]);
            if(counts == null) continue;
            counts["SUCCESS".equals(row[1]) ? 0 : 1] += ((Number) row[2]).longValue();
        }

        List<AppActivityResponse.WorkflowDaily> daily = new ArrayList<>();
        long success = 0, failure = 0;
        for(Map.Entry<LocalDate, long[]> entry : byDate.entrySet()) {
            daily.add(new AppActivityResponse.WorkflowDaily(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
            success += entry.getValue()[0];
            failure += entry.getValue()[1];
        }
        long[] todayCounts = byDate.get(today);

        Double avgDuration = null;
        long users = 0;
        List<Object[]> summary = workflowExecutionLogRepository.summary(applicationId, from);
        if(!summary.isEmpty() && summary.get(0) != null) {
            Object[] row = summary.get(0);
            avgDuration = row[0] == null ? null : ((Number) row[0]).doubleValue();
            users = row[1] == null ? 0 : ((Number) row[1]).longValue();
        }

        List<AppActivityResponse.TopWorkflow> top = workflowExecutionLogRepository
                .countByWorkflow(applicationId, from, PageRequest.of(0, TOP_LIMIT)).stream()
                .map(row -> {
                    String code = (String) row[0];
                    String displayName = workflowRepository.findByApplicationIdAndWorkflowCode(applicationId, code)
                            .map(Workflow::getDisplayName)
                            .orElse(null);
                    return new AppActivityResponse.TopWorkflow(code, displayName,
                            ((Number) row[1]).longValue(),
                            row[2] == null ? 0 : ((Number) row[2]).longValue());
                })
                .toList();

        return new AppActivityResponse.Workflow(success + failure, success, failure,
                todayCounts[0] + todayCounts[1], users, avgDuration, daily, top);
    }

    private AppActivityResponse.View view(String applicationId, LocalDate from, LocalDate today) {
        Map<LocalDate, long[]> byDate = emptyDays(from, today);
        for(Object[] row : viewAccessLogRepository.countDaily(applicationId, from)) {
            long[] counts = byDate.get((LocalDate) row[0]);
            if(counts == null) continue;
            counts[ViewAccessResult.ALLOWED.equals(row[1]) ? 0 : 1] += ((Number) row[2]).longValue();
        }

        List<AppActivityResponse.ViewDaily> daily = new ArrayList<>();
        long allowed = 0, denied = 0;
        for(Map.Entry<LocalDate, long[]> entry : byDate.entrySet()) {
            daily.add(new AppActivityResponse.ViewDaily(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
            allowed += entry.getValue()[0];
            denied += entry.getValue()[1];
        }

        List<AppActivityResponse.TopView> topViews = viewAccessLogRepository
                .countByView(applicationId, from, ViewAccessResult.ALLOWED, PageRequest.of(0, TOP_LIMIT)).stream()
                .map(row -> new AppActivityResponse.TopView((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();
        List<AppActivityResponse.TopMenu> topMenus = viewAccessLogRepository
                .countByMenu(applicationId, from, ViewAccessResult.ALLOWED, PageRequest.of(0, TOP_LIMIT)).stream()
                .map(row -> new AppActivityResponse.TopMenu((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();

        return new AppActivityResponse.View(allowed, denied, byDate.get(today)[0],
                viewAccessLogRepository.countUsers(applicationId, from, ViewAccessResult.ALLOWED),
                daily, topViews, topMenus);
    }

    private static Map<LocalDate, long[]> emptyDays(LocalDate from, LocalDate to) {
        Map<LocalDate, long[]> days = new LinkedHashMap<>();
        for(LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.put(date, new long[2]);
        }
        return days;
    }
}

package com.prometis.appbuilder.app.log;

import com.prometis.appbuilder.app.view.log.ViewAccessLogRepository;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 보관 기간(platform.log.retention-days)이 지난 워크플로우 실행 로그 / 화면 접근 로그를 매일 지운다.
 * 실행 시각은 platform.log.cleanup-cron (기본 매일 03:30, "-" 면 자동 정리를 끈다).
 * 서버를 여러 대 띄우면 각자 같은 삭제를 하지만, 지울 대상이 같아서 결과는 같다.
 */
@Slf4j
@Service
@EnableScheduling
@EnableConfigurationProperties(LogProperties.class)
@RequiredArgsConstructor
public class LogCleanupService {
    private final WorkflowExecutionLogRepository workflowExecutionLogRepository;
    private final ViewAccessLogRepository viewAccessLogRepository;
    private final LogProperties logProperties;

    @Scheduled(cron = "${platform.log.cleanup-cron:0 30 3 * * *}")
    public void scheduledCleanup() {
        try {
            cleanup(LocalDate.now());
        }
        catch (RuntimeException e) {
            log.warn("오래된 로그를 정리하지 못했습니다: {}", e.toString());
        }
    }

    /**
     * today 를 포함해 retentionDays 일 안의 로그만 남기고 그 앞 날짜의 로그를 지운다.
     * 예) retentionDays=90, today=11-22 → 08-25 부터 남기고 08-24 이전을 지운다.
     */
    @Transactional
    public CleanupResult cleanup(LocalDate today) {
        if(logProperties.keepForever()) {
            return new CleanupResult(null, 0, 0);
        }

        LocalDate keepFrom = today.minusDays(logProperties.retentionDays() - 1L);
        int workflowLogs = workflowExecutionLogRepository.deleteByDateBefore(keepFrom);
        int viewLogs = viewAccessLogRepository.deleteByDateBefore(keepFrom);

        if(workflowLogs > 0 || viewLogs > 0) {
            log.info("{} 이전 로그를 지웠습니다. 워크플로우 실행 로그 {}건, 화면 접근 로그 {}건", keepFrom, workflowLogs, viewLogs);
        }

        return new CleanupResult(keepFrom, workflowLogs, viewLogs);
    }

    /**
     * @param keepFrom 이 날짜부터 남겼다 (지우지 않았으면 null)
     */
    public record CleanupResult(LocalDate keepFrom, int workflowLogs, int viewLogs) {
    }
}

package com.prometis.appbuilder.app.log;

import com.prometis.appbuilder.app.view.log.ViewAccessLog;
import com.prometis.appbuilder.app.view.log.ViewAccessLogRepository;
import com.prometis.appbuilder.app.view.log.ViewAccessResult;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLog;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 보관 기간이 지난 로그만 지우는지 실제 DB(H2)로 확인한다.
 */
@DataJpaTest
class LogCleanupServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 22);

    @Autowired
    private WorkflowExecutionLogRepository workflowExecutionLogRepository;
    @Autowired
    private ViewAccessLogRepository viewAccessLogRepository;

    private LogCleanupService service(Integer retentionDays) {
        return new LogCleanupService(workflowExecutionLogRepository, viewAccessLogRepository, new LogProperties(retentionDays));
    }

    private void logsOn(LocalDate date) {
        workflowExecutionLogRepository.save(WorkflowExecutionLog.builder()
                .applicationId("ehr").workflowCode("wf").resultType("SUCCESS")
                .executedAt(date.atTime(12, 0)).executedDate(date)
                .build());
        viewAccessLogRepository.save(ViewAccessLog.builder()
                .applicationId("ehr").objectCode("emp").accessResult(ViewAccessResult.ALLOWED)
                .accessedAt(date.atTime(12, 0)).accessedDate(date)
                .build());
    }

    @Test
    void 보관기간_밖의_날짜만_지운다() {
        logsOn(TODAY);
        logsOn(TODAY.minusDays(2));   // 3일 보관의 첫날 → 남는다
        logsOn(TODAY.minusDays(3));   // 지운다
        logsOn(TODAY.minusDays(40));  // 지운다

        LogCleanupService.CleanupResult result = service(3).cleanup(TODAY);

        assertEquals(TODAY.minusDays(2), result.keepFrom());
        assertEquals(2, result.workflowLogs());
        assertEquals(2, result.viewLogs());

        List<LocalDate> left = workflowExecutionLogRepository.findAll().stream().map(WorkflowExecutionLog::getExecutedDate).sorted().toList();
        assertEquals(List.of(TODAY.minusDays(2), TODAY), left);
        assertEquals(2, viewAccessLogRepository.count());
    }

    @Test
    void 보관일수가_0_이하면_지우지_않는다() {
        logsOn(TODAY.minusDays(1000));

        LogCleanupService.CleanupResult result = service(0).cleanup(TODAY);

        assertNull(result.keepFrom());
        assertEquals(1, workflowExecutionLogRepository.count());
        assertEquals(1, viewAccessLogRepository.count());
    }

    @Test
    void 설정이_없으면_90일_보관이다() {
        assertEquals(90, new LogProperties(null).retentionDays());
    }
}

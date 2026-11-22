package com.prometis.appbuilder.console.app.log;

import com.prometis.appbuilder.app.view.log.ViewAccessLog;
import com.prometis.appbuilder.app.view.log.ViewAccessLogRepository;
import com.prometis.appbuilder.app.view.log.ViewAccessResult;
import com.prometis.appbuilder.app.workflow.code.WorkflowTrigger;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLog;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 콘솔 Log 화면의 조회 조건(기간, 부분 일치, 사용자, 결과, 실행 경로)과 애플리케이션 분리, 정렬, 페이지를 실제 DB(H2)로 확인한다.
 */
@DataJpaTest
@Import(LogSearchService.class)
class LogSearchServiceTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    @Autowired
    private LogSearchService logSearchService;
    @Autowired
    private WorkflowExecutionLogRepository workflowExecutionLogRepository;
    @Autowired
    private ViewAccessLogRepository viewAccessLogRepository;

    private void run(String app, String code, LocalDateTime at, boolean success, WorkflowTrigger trigger, Long userId, String userName) {
        workflowExecutionLogRepository.save(WorkflowExecutionLog.builder()
                .applicationId(app).workflowCode(code).triggerType(trigger)
                .userId(userId).userName(userName)
                .resultType(success ? "SUCCESS" : "ERROR")
                .executedAt(at).executedDate(at.toLocalDate())
                .build());
    }

    private void access(String objectCode, String objectName, String menuCd, String menuNm, LocalDateTime at, ViewAccessResult result) {
        viewAccessLogRepository.save(ViewAccessLog.builder()
                .applicationId("ehr").objectCode(objectCode).objectName(objectName)
                .menuCd(menuCd).menuNm(menuNm)
                .accessResult(result)
                .accessedAt(at).accessedDate(at.toLocalDate())
                .build());
    }

    @Test
    void 워크플로우_로그를_조건으로_찾고_최근_순으로_준다() {
        run("ehr", "getEmployee", DAY.atTime(9, 0), true, WorkflowTrigger.WEB, 1L, "Kim");
        run("ehr", "saveEmployee", DAY.atTime(10, 0), false, WorkflowTrigger.REST_API, 2L, "Lee");
        run("ehr", "getEmployee", DAY.minusDays(5).atTime(9, 0), true, WorkflowTrigger.SCHEDULER, null, null);
        run("other", "getEmployee", DAY.atTime(11, 0), true, WorkflowTrigger.WEB, 1L, "Kim");

        LogSearchService.LogPage<WorkflowExecutionLog> all =
                logSearchService.workflowLogs("ehr", null, null, null, null, null, null, null, null, null);
        assertEquals(3, all.totalElements());
        assertEquals(List.of("saveEmployee", "getEmployee", "getEmployee"),
                all.content().stream().map(WorkflowExecutionLog::getWorkflowCode).toList());

        assertEquals(2, logSearchService.workflowLogs("ehr", DAY, DAY, null, null, null, null, null, null, null).totalElements());
        assertEquals(2, logSearchService.workflowLogs("ehr", null, null, "GETEMP", null, null, null, null, null, null).totalElements());
        assertEquals(1, logSearchService.workflowLogs("ehr", null, null, null, null, "lee", null, null, null, null).totalElements());
        assertEquals(1, logSearchService.workflowLogs("ehr", null, null, null, null, "1", null, null, null, null).totalElements());
        assertEquals(1, logSearchService.workflowLogs("ehr", null, null, null, null, null, "ERROR", null, null, null).totalElements());
        assertEquals(1, logSearchService.workflowLogs("ehr", null, null, null, null, null, null, "SCHEDULER", null, null).totalElements());

        LogSearchService.LogPage<WorkflowExecutionLog> page = logSearchService.workflowLogs("ehr", null, null, null, null, null, null, null, 1, 2);
        assertEquals(3, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(1, page.content().size());
    }

    @Test
    void 화면_로그는_화면과_메뉴를_코드나_이름으로_찾는다() {
        access("emp", "사원 관리", "M01", "인사", DAY.atTime(9, 0), ViewAccessResult.ALLOWED);
        access("dept", "부서 관리", null, null, DAY.atTime(10, 0), ViewAccessResult.FORBIDDEN);

        assertEquals(1, logSearchService.viewLogs("ehr", null, null, "사원", null, null, null, null, null).totalElements());
        assertEquals(1, logSearchService.viewLogs("ehr", null, null, "dept", null, null, null, null, null).totalElements());
        assertEquals(1, logSearchService.viewLogs("ehr", null, null, null, "인사", null, null, null, null).totalElements());
        assertEquals(1, logSearchService.viewLogs("ehr", null, null, null, null, null, "FORBIDDEN", null, null).totalElements());
        // LIKE 특수문자는 글자 그대로 찾는다
        assertEquals(0, logSearchService.viewLogs("ehr", null, null, "%", null, null, null, null, null).totalElements());
    }

    @Test
    void 알_수_없는_조건_값은_거부한다() {
        assertThrows(LogSearchService.LogSearchException.class,
                () -> logSearchService.viewLogs("ehr", null, null, null, null, null, "NOPE", null, null));
        assertThrows(LogSearchService.LogSearchException.class,
                () -> logSearchService.workflowLogs("ehr", null, null, null, null, null, null, "NOPE", null, null));
    }
}

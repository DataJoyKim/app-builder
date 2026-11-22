package com.prometis.appbuilder.console.app.dashboard;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실행/접근 로그를 날짜별로 묶어 세는 집계 쿼리를 실제 DB(H2)로 확인한다.
 */
@DataJpaTest
@Import(AppActivityService.class)
class AppActivityServiceTest {
    private static final String APP = "ehr";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 22);

    @Autowired
    private AppActivityService appActivityService;
    @Autowired
    private WorkflowExecutionLogRepository workflowExecutionLogRepository;
    @Autowired
    private ViewAccessLogRepository viewAccessLogRepository;

    private void run(String applicationId, String workflowCode, LocalDate date, boolean success, Long userId, long durationMs) {
        workflowExecutionLogRepository.save(WorkflowExecutionLog.builder()
                .applicationId(applicationId)
                .workflowCode(workflowCode)
                .triggerType(WorkflowTrigger.WEB)
                .userId(userId)
                .resultType(success ? "SUCCESS" : "ERROR")
                .status(success ? 200 : 500)
                .durationMs(durationMs)
                .executedAt(date.atTime(10, 0))
                .executedDate(date)
                .build());
    }

    private void access(String applicationId, String objectCode, String menuCd, LocalDate date, ViewAccessResult result, Long userId) {
        LocalDateTime at = date.atTime(9, 0);
        viewAccessLogRepository.save(ViewAccessLog.builder()
                .applicationId(applicationId)
                .objectCode(objectCode)
                .objectName(objectCode + " 화면")
                .menuCd(menuCd)
                .menuNm(menuCd == null ? null : menuCd + " 메뉴")
                .userId(userId)
                .accessResult(result)
                .accessedAt(at)
                .accessedDate(date)
                .build());
    }

    @Test
    void 워크플로우_실행을_기간과_날짜별로_센다() {
        run(APP, "wfA", TODAY, true, 1L, 100);
        run(APP, "wfA", TODAY, false, 1L, 300);
        run(APP, "wfB", TODAY.minusDays(1), true, 2L, 200);
        run(APP, "wfB", TODAY.minusDays(7), true, 3L, 100);   // 7일 기간 밖
        run("other", "wfA", TODAY, true, 9L, 100);            // 다른 애플리케이션

        AppActivityResponse result = appActivityService.activity(APP, 7, TODAY);

        assertEquals(7, result.days());
        assertEquals(TODAY.minusDays(6), result.from());
        AppActivityResponse.Workflow wf = result.workflow();
        assertEquals(3, wf.total());
        assertEquals(2, wf.success());
        assertEquals(1, wf.failure());
        assertEquals(2, wf.today());
        assertEquals(2, wf.users());
        assertEquals(200.0, wf.avgDurationMs(), 0.001);

        assertEquals(7, wf.daily().size());
        AppActivityResponse.WorkflowDaily last = wf.daily().get(6);
        assertEquals(TODAY, last.date());
        assertEquals(1, last.success());
        assertEquals(1, last.failure());
        assertEquals(0, wf.daily().get(0).success() + wf.daily().get(0).failure());

        assertEquals("wfA", wf.top().get(0).workflowCode());
        assertEquals(2, wf.top().get(0).count());
        assertEquals(1, wf.top().get(0).failure());
    }

    @Test
    void 화면_접근을_허용과_거부로_나눠_세고_메뉴별로_묶는다() {
        access(APP, "emp", "M01", TODAY, ViewAccessResult.ALLOWED, 1L);
        access(APP, "emp", "M01", TODAY, ViewAccessResult.ALLOWED, 2L);
        access(APP, "emp", null, TODAY.minusDays(2), ViewAccessResult.ALLOWED, 1L);
        access(APP, "dept", "M02", TODAY, ViewAccessResult.ALLOWED, null);
        access(APP, "dept", "M02", TODAY, ViewAccessResult.FORBIDDEN, 3L);

        AppActivityResponse.View view = appActivityService.activity(APP, null, TODAY).view();

        assertEquals(4, view.total());
        assertEquals(1, view.denied());
        assertEquals(3, view.today());
        assertEquals(2, view.users());
        assertEquals(14, view.daily().size());

        assertEquals("emp", view.topViews().get(0).objectCode());
        assertEquals(3, view.topViews().get(0).count());
        assertEquals("M01", view.topMenus().get(0).menuCd());
        assertEquals(2, view.topMenus().get(0).count());
        assertEquals(1, view.topMenus().get(1).count());
    }

    @Test
    void 기록이_없으면_0과_빈_목록이다() {
        AppActivityResponse result = appActivityService.activity(APP, 30, TODAY);

        assertEquals(0, result.workflow().total());
        assertNull(result.workflow().avgDurationMs());
        assertTrue(result.workflow().top().isEmpty());
        assertEquals(30, result.view().daily().size());
        assertEquals(0, result.view().users());
    }
}

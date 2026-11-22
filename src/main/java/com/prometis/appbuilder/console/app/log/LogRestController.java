package com.prometis.appbuilder.console.app.log;

import com.prometis.appbuilder.app.view.log.ViewAccessLog;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 애플리케이션 콘솔 > Log 화면(console/app/contents/log.html)의 API. 워크플로우 실행 로그와 화면 접근 로그를 조회한다.
 */
@RestController
@RequestMapping("/{applicationId}/console/api/log")
@RequiredArgsConstructor
public class LogRestController {
    private final LogSearchService logSearchService;

    @GetMapping("/workflow")
    public ResponseEntity<LogSearchService.LogPage<WorkflowExecutionLog>> workflowLogs(
            @PathVariable("applicationId") String applicationId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "workflow", required = false) String workflow,
            @RequestParam(value = "object", required = false) String object,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "resultType", required = false) String resultType,
            @RequestParam(value = "trigger", required = false) String trigger,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(logSearchService.workflowLogs(applicationId, from, to, workflow, object, user, resultType, trigger, page, size));
    }

    @GetMapping("/view")
    public ResponseEntity<LogSearchService.LogPage<ViewAccessLog>> viewLogs(
            @PathVariable("applicationId") String applicationId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "object", required = false) String object,
            @RequestParam(value = "menu", required = false) String menu,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "result", required = false) String result,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(logSearchService.viewLogs(applicationId, from, to, object, menu, user, result, page, size));
    }

    @ExceptionHandler(LogSearchService.LogSearchException.class)
    public ResponseEntity<Map<String, String>> badRequest(LogSearchService.LogSearchException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

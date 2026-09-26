package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.scheduler.SchedulerManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("console.SchedulerRestController")
@RequestMapping("/console/api/scheduler")
public class SchedulerRestController {
    @Autowired
    private SchedulerManagementService schedulerManagementService;

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        try {
            return new ResponseEntity<>(Map.of("started", schedulerManagementService.isStarted()), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/start")
    public ResponseEntity<?> start() {
        try {
            schedulerManagementService.start();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stop() {
        try {
            schedulerManagementService.standby();
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/cron-preview")
    public ResponseEntity<?> cronPreview(@RequestBody Map<String,Object> params) {
        String cronExpression = (String) params.get("cronExpression");

        try {
            List<Date> nextFireTimes = schedulerManagementService.previewNextFireTimes(cronExpression, 5);

            List<LocalDateTime> results = nextFireTimes.stream()
                    .map(SchedulerManagementService::toLocalDateTime)
                    .collect(Collectors.toList());

            return new ResponseEntity<>(results, HttpStatus.OK);
        }
        catch (ParseException e) {
            return new ResponseEntity<>(Collections.singletonMap("message", "올바르지 않은 cron 표현식입니다: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // Map.of는 value가 null이면 그 자리에서 NullPointerException을 던지므로,
    // 메시지가 없는 예외(getMessage()==null)에서도 안전하게 에러 응답을 만들기 위한 헬퍼.
    private static Map<String, String> errorBody(Exception e) {
        String message = (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
        return Collections.singletonMap("message", message);
    }
}

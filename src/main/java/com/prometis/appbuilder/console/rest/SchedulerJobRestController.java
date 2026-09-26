package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.scheduler.SchedulerJobHistoryRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobWorkflowRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerManagementService;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJobHistory;
import com.prometis.appbuilder.app.scheduler.dto.SchedulerJobSummaryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("console.SchedulerJobRestController")
@RequestMapping("/console/api/scheduler/job")
public class SchedulerJobRestController {
    @Autowired
    private SchedulerJobRepository repository;
    @Autowired
    private SchedulerJobWorkflowRepository schedulerJobWorkflowRepository;
    @Autowired
    private SchedulerJobHistoryRepository schedulerJobHistoryRepository;
    @Autowired
    private SchedulerManagementService schedulerManagementService;

    @GetMapping("")
    public ResponseEntity<?> getList() {
        List<SchedulerJobSummaryDto> results = repository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    private SchedulerJobSummaryDto toSummary(SchedulerJob job) {
        LocalDateTime lastExecutedAt = schedulerJobHistoryRepository.findFirstBySchedulerJobIdOrderByStartedAtDesc(job.getId())
                .map(SchedulerJobHistory::getStartedAt)
                .orElse(null);

        Date nextFireTime = schedulerManagementService.getNextFireTime(job.getJobCode());
        LocalDateTime nextFireDateTime = (nextFireTime != null) ? SchedulerManagementService.toLocalDateTime(nextFireTime) : null;

        return SchedulerJobSummaryDto.of(job, lastExecutedAt, nextFireDateTime);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        SchedulerJob results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {
        SchedulerJob createdData = SchedulerJob.builder()
                .jobCode((String) params.get("jobCode"))
                .jobName((String) params.get("jobName"))
                .cronExpression((String) params.get("cronExpression"))
                .description((String) params.get("description"))
                .enabled(Boolean.TRUE.equals(params.get("enabled")))
                .build();

        SchedulerJob savedData = repository.save(createdData);

        try {
            schedulerManagementService.scheduleOrReplace(savedData);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(savedData, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        SchedulerJob savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        String previousJobCode = savedData.getJobCode();

        savedData.update(
                (String) params.get("jobCode"),
                (String) params.get("jobName"),
                (String) params.get("cronExpression"),
                (String) params.get("description"),
                Boolean.TRUE.equals(params.get("enabled"))
        );

        repository.save(savedData);

        try {
            // jobCode는 Quartz JobKey로도 쓰이므로, 변경됐다면 예전 키로 등록된 Job을 먼저 내려야 고아 Job이 남지 않는다.
            if(!previousJobCode.equals(savedData.getJobCode())) {
                schedulerManagementService.unscheduleJob(previousJobCode);
            }

            schedulerManagementService.scheduleOrReplace(savedData);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        SchedulerJob savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        try {
            schedulerManagementService.unscheduleJob(savedData.getJobCode());
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        schedulerJobWorkflowRepository.deleteBySchedulerJobId(savedData.getId());
        schedulerJobHistoryRepository.deleteBySchedulerJobId(savedData.getId());
        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable("id") Long id) {
        SchedulerJob job = repository.findById(id).orElseThrow(RuntimeException::new);

        // enabled=false로 영속화해둬야 재기동 시(SchedulerBootstrap) 다시 활성화되지 않는다.
        job.update(job.getJobCode(), job.getJobName(), job.getCronExpression(), job.getDescription(), false);
        repository.save(job);

        try {
            schedulerManagementService.pauseJob(job.getJobCode());
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable("id") Long id) {
        SchedulerJob job = repository.findById(id).orElseThrow(RuntimeException::new);

        job.update(job.getJobCode(), job.getJobName(), job.getCronExpression(), job.getDescription(), true);
        repository.save(job);

        try {
            schedulerManagementService.resumeJob(job);
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/restart")
    public ResponseEntity<?> restart(@PathVariable("id") Long id) {
        SchedulerJob job = repository.findById(id).orElseThrow(RuntimeException::new);

        try {
            schedulerManagementService.restartJob(job);
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<?> trigger(@PathVariable("id") Long id) {
        SchedulerJob job = repository.findById(id).orElseThrow(RuntimeException::new);

        try {
            schedulerManagementService.triggerNow(job.getJobCode());
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }
        catch (Exception e) {
            return new ResponseEntity<>(errorBody(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> history(@PathVariable("id") Long id) {
        return new ResponseEntity<>(schedulerJobHistoryRepository.findTop50BySchedulerJobIdOrderByStartedAtDesc(id), HttpStatus.OK);
    }

    // Map.of는 value가 null이면 그 자리에서 NullPointerException을 던지므로,
    // 메시지가 없는 예외(getMessage()==null)에서도 안전하게 에러 응답을 만들기 위한 헬퍼.
    private static Map<String, String> errorBody(Exception e) {
        String message = (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
        return Collections.singletonMap("message", message);
    }
}

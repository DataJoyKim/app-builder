package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.scheduler.SchedulerJobRepository;
import com.prometis.appbuilder.app.scheduler.SchedulerJobWorkflowRepository;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJobWorkflow;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.SchedulerJobWorkflowRestController")
@RequestMapping("/{applicationId}/console/api/scheduler/job/workflow")
public class SchedulerJobWorkflowRestController {
    @Autowired
    private SchedulerJobWorkflowRepository repository;
    @Autowired
    private SchedulerJobRepository schedulerJobRepository;
    @Autowired
    private WorkflowRepository workflowRepository;

    @GetMapping("/{schedulerJobId}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("schedulerJobId") Long schedulerJobId) {
        List<SchedulerJobWorkflow> results = repository.findBySchedulerJobIdOrderByOrderNum(schedulerJobId).stream()
                .filter(owned -> owned.getSchedulerJob() != null && applicationId.equals(owned.getSchedulerJob().getApplicationId()))
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Long schedulerJobId = Long.valueOf((String) params.get("schedulerJobId"));
        Long workflowId = Long.valueOf(String.valueOf(params.get("workflowId")));

        SchedulerJob schedulerJob = schedulerJobRepository.findById(schedulerJobId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Workflow workflow = workflowRepository.findById(workflowId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        SchedulerJobWorkflow createdData = SchedulerJobWorkflow.builder()
                .schedulerJob(schedulerJob)
                .workflow(workflow)
                .requestMessageJson((String) params.get("requestMessageJson"))
                .orderNum(Integer.valueOf((String) params.get("orderNum")))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        SchedulerJobWorkflow savedData = repository.findById(id)
                .filter(owned -> owned.getSchedulerJob() != null && applicationId.equals(owned.getSchedulerJob().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Long workflowId = Long.valueOf(String.valueOf(params.get("workflowId")));

        Workflow workflow = workflowRepository.findById(workflowId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        savedData.update(
                workflow,
                (String) params.get("requestMessageJson"),
                Integer.valueOf((String) params.get("orderNum"))
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        SchedulerJobWorkflow savedData = repository.findById(id)
                .filter(owned -> owned.getSchedulerJob() != null && applicationId.equals(owned.getSchedulerJob().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}

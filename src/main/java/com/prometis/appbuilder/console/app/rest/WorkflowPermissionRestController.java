package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowPermission;
import com.prometis.appbuilder.app.workflow.WorkflowPermissionRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController("console.WorkflowPermissionRestController")
@RequestMapping("/{applicationId}/console/api/workflow-permission")
public class WorkflowPermissionRestController {
    @Autowired
    private WorkflowPermissionRepository repository;
    @Autowired
    private WorkflowRepository workflowRepository;

    @GetMapping("")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @RequestParam Map<String,Object> params) {

        Long workflowId = Long.valueOf((String) params.get("workflowId"));

        Workflow workflow = workflowRepository.findById(workflowId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow();

        List<WorkflowPermission> results = repository.findByWorkflow(workflow).stream()
                .filter(owned -> owned.getWorkflow() != null && applicationId.equals(owned.getWorkflow().getApplicationId()))
                .toList();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Long workflowId = Long.valueOf((String) params.get("workflowId"));

        Workflow workflow = workflowRepository.findById(workflowId)
                                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                                .orElseThrow();

        WorkflowPermission createdData = WorkflowPermission.builder()
                .permissionCode((String) params.get("permissionCode"))
                .workflow(workflow)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        WorkflowPermission savedData = repository.findById(id)
                .filter(owned -> owned.getWorkflow() != null && applicationId.equals(owned.getWorkflow().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Long workflowId = Long.valueOf((String) params.get("workflowId"));

        Workflow workflow = workflowRepository.findById(workflowId)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow();

        savedData.update(
                (String) params.get("permissionCode"),
                workflow
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        WorkflowPermission savedData = repository.findById(id)
                .filter(owned -> owned.getWorkflow() != null && applicationId.equals(owned.getWorkflow().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}

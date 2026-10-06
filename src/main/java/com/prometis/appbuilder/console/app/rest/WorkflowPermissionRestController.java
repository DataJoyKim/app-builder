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

    // 권한 화면: 이 권한으로 실행할 수 있는 워크플로우들
    @GetMapping("/by-permission")
    public ResponseEntity<?> getByPermission(@PathVariable("applicationId") String applicationId, @RequestParam("permissionCode") String permissionCode) {
        List<WorkflowPermission> results = repository.findByPermissionCodeAndWorkflow_ApplicationId(permissionCode, applicationId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Long workflowId = idOf(params.get("workflowId"));

        Workflow workflow = workflowRepository.findById(workflowId)
                                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                                .orElseThrow();

        String permissionCode = (String) params.get("permissionCode");
        if(permissionCode == null || permissionCode.isBlank()) {
            return new ResponseEntity<>(Map.of("message", "권한을 선택해주세요."), HttpStatus.BAD_REQUEST);
        }
        // 같은 워크플로우에 같은 권한을 두 번 주지 않는다
        if(repository.existsByWorkflowIdAndPermissionCode(workflow.getId(), permissionCode)) {
            return new ResponseEntity<>(Map.of("message", "이미 이 권한으로 실행할 수 있는 워크플로우입니다."), HttpStatus.BAD_REQUEST);
        }

        WorkflowPermission createdData = WorkflowPermission.builder()
                .permissionCode(permissionCode)
                .workflow(workflow)
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        WorkflowPermission savedData = repository.findById(id)
                .filter(owned -> owned.getWorkflow() != null && applicationId.equals(owned.getWorkflow().getApplicationId()))
                .orElseThrow(RuntimeException::new);

        Long workflowId = idOf(params.get("workflowId"));

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

    // 화면에 따라 id 를 문자열("3") 또는 숫자(3)로 보낸다
    private static Long idOf(Object value) {
        return Long.valueOf(String.valueOf(value));
    }
}

package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroup;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroupRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 워크플로우에 매핑된 IP 그룹 조회. 저장은 워크플로우 저장(/console/api/workflow/save)에서 함께 처리한다.
 */
@RestController("console.WorkflowIpGroupRestController")
@RequestMapping("/console/api/workflow-ip-group")
public class WorkflowIpGroupRestController {
    @Autowired
    private WorkflowIpGroupRepository repository;
    @Autowired
    private WorkflowRepository workflowRepository;

    @GetMapping("")
    public ResponseEntity<?> get(@RequestParam Map<String,Object> params) {
        Long workflowId = Long.valueOf((String) params.get("workflowId"));

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow();

        List<WorkflowIpGroup> results = repository.findByWorkflow(workflow);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}

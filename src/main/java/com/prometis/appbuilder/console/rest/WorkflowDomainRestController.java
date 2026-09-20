package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.workflow.Workflow;
import com.prometis.appbuilder.workflow.WorkflowDomain;
import com.prometis.appbuilder.workflow.WorkflowDomainRepository;
import com.prometis.appbuilder.workflow.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 워크플로우에 매핑된 허용 도메인 조회. 저장은 워크플로우 저장(/console/api/workflow/save)에서 함께 처리한다.
 */
@RestController("console.WorkflowDomainRestController")
@RequestMapping("/console/api/workflow-domain")
public class WorkflowDomainRestController {
    @Autowired
    private WorkflowDomainRepository repository;
    @Autowired
    private WorkflowRepository workflowRepository;

    @GetMapping("")
    public ResponseEntity<?> get(@RequestParam Map<String,Object> params) {
        Long workflowId = Long.valueOf((String) params.get("workflowId"));

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow();

        List<WorkflowDomain> results = repository.findByWorkflow(workflow);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}

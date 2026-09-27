package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.workflow.WorkflowEdge;
import com.prometis.appbuilder.app.workflow.WorkflowEdgeRepository;
import com.prometis.appbuilder.console.app.AppConsoleWorkflowScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 워크플로우 노드 연결정보 조회용. 저장은 노드와 함께 통째로 갈아끼워야 해서 /console/api/workflow/save 에서 처리한다.
 */
@RestController("console.WorkflowEdgeRestController")
@RequestMapping("/{applicationId}/console/api/workflow-edge")
public class WorkflowEdgeRestController {
    @Autowired
    private WorkflowEdgeRepository repository;
    @Autowired
    private AppConsoleWorkflowScope appConsoleWorkflowScope;

    @GetMapping("")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @RequestParam Map<String,Object> params) {
        Long workflowId = Long.valueOf((String) params.get("workflowId"));

        if(!appConsoleWorkflowScope.owns(applicationId, workflowId)) {
            return new ResponseEntity<>(List.of(), HttpStatus.OK);
        }

        List<WorkflowEdge> results = repository.findByWorkflowIdOrderByOrderNum(workflowId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}

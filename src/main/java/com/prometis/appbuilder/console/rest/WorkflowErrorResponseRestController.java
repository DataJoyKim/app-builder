package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.workflow.WorkflowErrorResponse;
import com.prometis.appbuilder.app.workflow.WorkflowErrorResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 에러메시지 노드의 설정 조회용. 저장은 노드/연결정보와 함께 통째로 갈아끼워야 해서 /console/api/workflow/save 에서 처리한다.
 */
@RestController("console.WorkflowErrorResponseRestController")
@RequestMapping("/console/api/workflow-error-response")
public class WorkflowErrorResponseRestController {
    @Autowired
    private WorkflowErrorResponseRepository repository;

    @GetMapping("")
    public ResponseEntity<?> get(@RequestParam Map<String,Object> params) {
        Long workflowId = Long.valueOf((String) params.get("workflowId"));

        List<WorkflowErrorResponse> results = repository.findByWorkflowId(workflowId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}

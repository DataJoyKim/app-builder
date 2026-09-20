package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.node.WorkflowNode;
import com.prometis.appbuilder.node.WorkflowNodeRepository;
import com.prometis.appbuilder.node.code.FunctionType;
import com.prometis.appbuilder.util.DataTypeUtil;
import com.prometis.appbuilder.workflow.Workflow;
import com.prometis.appbuilder.workflow.WorkflowAuthority;
import com.prometis.appbuilder.workflow.WorkflowAuthorityRepository;
import com.prometis.appbuilder.workflow.WorkflowCondition;
import com.prometis.appbuilder.workflow.WorkflowConditionRepository;
import com.prometis.appbuilder.workflow.WorkflowEdge;
import com.prometis.appbuilder.workflow.WorkflowEdgeRepository;
import com.prometis.appbuilder.workflow.WorkflowErrorResponse;
import com.prometis.appbuilder.workflow.WorkflowErrorResponseRepository;
import com.prometis.appbuilder.workflow.WorkflowIpGroup;
import com.prometis.appbuilder.workflow.WorkflowIpGroupRepository;
import com.prometis.appbuilder.workflow.WorkflowRepository;
import com.prometis.appbuilder.workflow.code.BranchType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("console.WorkflowRestController")
@RequestMapping("/console/api/workflow")
public class WorkflowRestController {
    @Autowired
    private WorkflowRepository repository;
    @Autowired
    private WorkflowNodeRepository workflowNodeRepository;
    @Autowired
    private WorkflowEdgeRepository workflowEdgeRepository;
    @Autowired
    private WorkflowConditionRepository workflowConditionRepository;
    @Autowired
    private WorkflowErrorResponseRepository workflowErrorResponseRepository;
    @Autowired
    private WorkflowAuthorityRepository workflowAuthorityRepository;
    @Autowired
    private WorkflowIpGroupRepository workflowIpGroupRepository;

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody Map<String,Object> params) {
        Map<String,Object> workflowParams = (Map<String, Object>) params.get("workflow");
        List<Map<String,Object>> workflowNodeParams = (List<Map<String,Object>>) params.get("workflowNodes");
        List<Map<String,Object>> workflowEdgeParams = (List<Map<String,Object>>) params.get("workflowEdges");
        List<Map<String,Object>> workflowConditionParams = (List<Map<String,Object>>) params.get("workflowConditions");
        List<Map<String,Object>> workflowErrorResponseParams = (List<Map<String,Object>>) params.get("workflowErrorResponses");
        List<Map<String,Object>> workflowAuthorityParams = (List<Map<String,Object>>) params.get("workflowAuthority");
        List<Map<String,Object>> workflowIpGroupParams = (List<Map<String,Object>>) params.get("workflowIpGroup");

        Long id = (workflowParams.get("id") == null || ((String) workflowParams.get("id")).isEmpty())
                ? null
                : Long.valueOf((String) workflowParams.get("id"));

        Workflow workflow;

        if (id == null) {
            workflow = Workflow.builder()
                    .workflowCode((String) workflowParams.get("workflowCode"))
                    .displayName((String) workflowParams.get("displayName"))
                    .note((String) workflowParams.get("note"))
                    .useAuthValidation((Boolean) workflowParams.get("useAuthValidation"))
                    .build();
        }
        else {
            workflow = repository.findById(id)
                    .orElseThrow();

            workflow.update(
                    (String) workflowParams.get("workflowCode"),
                    (String) workflowParams.get("displayName"),
                    (String) workflowParams.get("note"),
                    (Boolean) workflowParams.get("useAuthValidation")
            );
        }

        Workflow savedWorkflow = repository.save(workflow);

        workflowNodeRepository.deleteByWorkflowId(savedWorkflow.getId());

        int nodeSeq = 0;
        for(Map<String,Object> param : workflowNodeParams) {
            nodeSeq++;

            FunctionType functionType = FunctionType.valueOf((String) param.get("functionType"));
            String nodeId = resolveNodeId((String) param.get("nodeId"), nodeSeq);

            WorkflowNode workflowNode = WorkflowNode.builder()
                    .workflowId(savedWorkflow.getId())
                    .nodeId(nodeId)
                    // 조건분기/에러메시지 노드는 별도로 만들어둔 기능이 없으므로 노드 식별자를 기능명으로 대신 채운다.
                    .functionName(resolveFunctionName((String) param.get("functionName"), functionType, nodeId))
                    .functionType(functionType)
                    .orderNum((Integer) param.get("orderNum"))
                    .isLogging((Boolean) param.get("isLogging"))
                    .requestMessageId((String) param.get("requestMessageId"))
                    .responseMessageId((String) param.get("responseMessageId"))
                    // 캔버스 배치는 빌더 화면에서만 쓰는 값이라 없으면 없는 대로 저장한다.
                    .positionX(DataTypeUtil.valueIntegerOf(param.get("positionX")))
                    .positionY(DataTypeUtil.valueIntegerOf(param.get("positionY")))
                    .build();

            workflowNodeRepository.save(workflowNode);
        }

        workflowEdgeRepository.deleteByWorkflowId(savedWorkflow.getId());

        if(workflowEdgeParams != null) {
            int edgeSeq = 0;
            for(Map<String,Object> param : workflowEdgeParams) {
                WorkflowEdge workflowEdge = WorkflowEdge.builder()
                        .workflowId(savedWorkflow.getId())
                        .sourceNodeId((String) param.get("sourceNodeId"))
                        .targetNodeId((String) param.get("targetNodeId"))
                        .branchType(resolveBranchType((String) param.get("branchType")))
                        .branchId((String) param.get("branchId"))
                        .orderNum(param.get("orderNum") == null ? edgeSeq : (Integer) param.get("orderNum"))
                        .build();

                workflowEdgeRepository.save(workflowEdge);

                edgeSeq++;
            }
        }

        workflowConditionRepository.deleteByWorkflowId(savedWorkflow.getId());

        if(workflowConditionParams != null) {
            int conditionSeq = 0;
            for(Map<String,Object> param : workflowConditionParams) {
                WorkflowCondition workflowCondition = WorkflowCondition.builder()
                        .workflowId(savedWorkflow.getId())
                        .nodeId((String) param.get("nodeId"))
                        .branchId((String) param.get("branchId"))
                        .conditionExpression((String) param.get("conditionExpression"))
                        .orderNum(param.get("orderNum") == null ? conditionSeq : (Integer) param.get("orderNum"))
                        .build();

                workflowConditionRepository.save(workflowCondition);

                conditionSeq++;
            }
        }

        workflowErrorResponseRepository.deleteByWorkflowId(savedWorkflow.getId());

        if(workflowErrorResponseParams != null) {
            for(Map<String,Object> param : workflowErrorResponseParams) {
                WorkflowErrorResponse workflowErrorResponse = WorkflowErrorResponse.builder()
                        .workflowId(savedWorkflow.getId())
                        .nodeId((String) param.get("nodeId"))
                        .status(DataTypeUtil.valueIntegerOf(param.get("status")))
                        .code((String) param.get("code"))
                        .message((String) param.get("message"))
                        .contents((String) param.get("contents"))
                        .build();

                workflowErrorResponseRepository.save(workflowErrorResponse);
            }
        }

        workflowAuthorityRepository.deleteByWorkflowId(savedWorkflow.getId());

        for(Map<String,Object> param : workflowAuthorityParams) {
            WorkflowAuthority workflowAuthority = WorkflowAuthority.builder()
                    .authorityCode((String) param.get("authorityCode"))
                    .workflow(savedWorkflow)
                    .build();

            workflowAuthorityRepository.save(workflowAuthority);
        }

        // IP 접근제어. 매핑한 IP 그룹이 없으면 IP 제한을 쓰지않는 워크플로우가 된다.
        workflowIpGroupRepository.deleteByWorkflowId(savedWorkflow.getId());

        if(workflowIpGroupParams != null) {
            for(Map<String,Object> param : workflowIpGroupParams) {
                WorkflowIpGroup workflowIpGroup = WorkflowIpGroup.builder()
                        .ipGroupCode((String) param.get("ipGroupCode"))
                        .workflow(savedWorkflow)
                        .build();

                workflowIpGroupRepository.save(workflowIpGroup);
            }
        }

        return ResponseEntity.ok(savedWorkflow);
    }

    private static String resolveNodeId(String nodeId, int nodeSeq) {
        return (nodeId == null || nodeId.isBlank()) ? "node-" + nodeSeq : nodeId;
    }

    private static String resolveFunctionName(String functionName, FunctionType functionType, String nodeId) {
        if(functionName != null && !functionName.isBlank()) {
            return functionName;
        }

        boolean isControlNode = FunctionType.CONDITION.equals(functionType) || FunctionType.ERROR_MESSAGE.equals(functionType);

        return isControlNode ? nodeId : functionName;
    }

    private static BranchType resolveBranchType(String branchType) {
        return (branchType == null || branchType.isBlank()) ? BranchType.DEFAULT : BranchType.valueOf(branchType);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        Workflow workflow = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        workflowNodeRepository.deleteByWorkflowId(workflow.getId());
        workflowEdgeRepository.deleteByWorkflowId(workflow.getId());
        workflowConditionRepository.deleteByWorkflowId(workflow.getId());
        workflowErrorResponseRepository.deleteByWorkflowId(workflow.getId());
        workflowAuthorityRepository.deleteByWorkflowId(workflow.getId());
        workflowIpGroupRepository.deleteByWorkflowId(workflow.getId());
        repository.deleteById(workflow.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> get() {
        List<Workflow> results = repository.findAll();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        Workflow results = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {

        Workflow workflowBuilder = Workflow.builder()
                .workflowCode((String) params.get("workflowCode"))
                .displayName((String) params.get("displayName"))
                .note((String) params.get("note"))
                .useAuthValidation((Boolean) params.get("useAuthValidation"))
                .build();

        Workflow results = repository.save(workflowBuilder);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateService(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        Workflow workflowBuilder = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        workflowBuilder.update(
                (String) params.get("workflowCode"),
                (String) params.get("displayName"),
                (String) params.get("note"),
                (Boolean) params.get("useAuthValidation")
        );

        Workflow results = repository.save(workflowBuilder);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}

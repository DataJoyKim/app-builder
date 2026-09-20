package com.prometis.appbuilder.workflow;

import com.prometis.appbuilder.dto.RequestMessage;
import com.prometis.appbuilder.dto.ResponseMessage;
import com.prometis.appbuilder.executor.script.ScriptEngineExecuteException;
import com.prometis.appbuilder.node.*;
import com.prometis.appbuilder.node.code.ResultType;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.appbuilder.security.domain.GrantedAuthority;
import com.prometis.appbuilder.security.exception.SecurityBusinessException;
import com.prometis.appbuilder.security.ip.IpAccessValidator;
import com.prometis.appbuilder.security.service.AuthService;
import com.prometis.appbuilder.security.token.TokenCookie;
import com.prometis.appbuilder.workflow.code.BranchType;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final WorkflowEdgeRepository workflowEdgeRepository;
    private final WorkflowConditionRepository workflowConditionRepository;
    private final WorkflowErrorResponseRepository workflowErrorResponseRepository;
    private final WorkflowAuthorityRepository workflowAuthorityRepository;
    private final NodeExecutorFactory nodeExecutorFactory;
    private final ConditionEvaluator conditionEvaluator;
    private final AuthService authService;
    private final IpAccessValidator ipAccessValidator;

    public ResponseMessage execute(
            HttpServletRequest request,
            HttpServletResponse response,
            RequestMessage requestMessage
    ) {
        try {
            Workflow workflow = findWorkflow(requestMessage);

            AuthenticatedUser user = null;

            if(workflow.getUseAuthValidation()) {
                user = authService.authentication(TokenCookie.resolveAccessToken(request));
            }

            if(user != null) {
                validateAuthorization(user, workflow);
            }

            // IP 접근제어.
            ipAccessValidator.validate(request, workflow);

            return executeFunction(requestMessage, user, createGraph(workflow));
        }
        catch (SecurityBusinessException e) {
            return ResponseMessage.createErrorMessage(e.getStatus(), e.getErrorCode(), e.getErrorMsg());
        }
        catch (BusinessException e) {
            return ResponseMessage.createErrorMessage(e.getStatus(), e.getCode(), e.getMsg());
        }
    }

    public void validateAuthorization(AuthenticatedUser user, Workflow workflow) throws BusinessException {
        List<WorkflowAuthority> workflowAuthorities = workflowAuthorityRepository.findByWorkflow(workflow);
        if(workflowAuthorities.isEmpty()) {
            throw new BusinessException(WorkflowErrorMessage.NOT_SETTING_AUTHORITY);
        }

        Map<String, Workflow> authorityMap = new HashMap<>();
        for(WorkflowAuthority workflowAuthority : workflowAuthorities) {
            authorityMap.put(workflowAuthority.getAuthorityCode(), workflowAuthority.getWorkflow());
        }

        if(authorityMap.containsKey(WorkflowAuthority.VALID_PASS)) {
            return;
        }

        List<GrantedAuthority> grantedAuthorities = user.getGrantedAuthorities();
        if(grantedAuthorities.isEmpty()) {
            throw new BusinessException(WorkflowErrorMessage.NOT_HAS_AUTHORITIES);
        }

        boolean hasAuthority = false;
        for(GrantedAuthority authority : grantedAuthorities){
            if(authorityMap.containsKey(authority.getRole())) {
                hasAuthority = true;
                break;
            }
        }

        if(!hasAuthority) {
            throw new BusinessException(WorkflowErrorMessage.PERMISSION_DENIED);
        }
    }

    private Workflow findWorkflow(RequestMessage requestMessage) throws BusinessException {
        Optional<Workflow> opWorkflow = workflowRepository.findByWorkflowCode(requestMessage.getHeader().getWorkflowCode());
        if(opWorkflow.isEmpty()) {
            throw new BusinessException(WorkflowErrorMessage.NOT_FOUND_WORKFLOW);
        }

        return opWorkflow.get();
    }

    private WorkflowGraph createGraph(Workflow workflow) {
        return WorkflowGraph.of(
                workflowNodeRepository.findByWorkflowId(workflow.getId()),
                workflowEdgeRepository.findByWorkflowId(workflow.getId()),
                workflowConditionRepository.findByWorkflowId(workflow.getId()),
                workflowErrorResponseRepository.findByWorkflowId(workflow.getId())
        );
    }

    // 시작 노드부터 연결정보(WorkflowEdge)를 따라가며 실행한다.
    // 조건분기 노드는 걸린 조건의 가지 하나로만 이어지므로 나머지 가지의 노드는 실행되지 않는다.
    private ResponseMessage executeFunction(
            RequestMessage requestMessage,
            AuthenticatedUser user,
            WorkflowGraph graph
    ) throws BusinessException {
        Map<String, List<Map<String, Object>>> messageStorage = requestMessage.getBody();

        List<WorkflowNode> executedFunctions = new ArrayList<>();
        int failureCnt = 0;
        int step = 0;

        WorkflowNode current = graph.getStartNode();

        while(current != null) {
            if(++step > WorkflowGraph.MAX_EXECUTE_STEP) {
                throw new BusinessException(WorkflowErrorMessage.EXCEED_MAX_EXECUTE_STEP);
            }

            List<Map<String, Object>> params = messageStorage.get(current.getRequestMessageId());
            if(params == null) {
                params = new ArrayList<>();
            }

            if(current.isCondition()) {
                current = nextOfCondition(graph, current, params);
                continue;
            }

            // 에러메시지 노드에 닿으면 뒤로 이어진 노드가 있더라도 더 실행하지 않고 설정된 에러응답으로 끝낸다.
            if(current.isErrorMessage()) {
                return errorResponseOf(graph, current, messageStorage);
            }

            NodeExecutor executor = nodeExecutorFactory.instance(current.getFunctionType());

            NodeResult result = executor.execute(user, current.getFunctionName(), requestMessage.getHeader(), params);

            if(ResultType.FAILURE.equals(result.getResultType())) {
                failureCnt++;
            }

            messageStorage.put(current.getResponseMessageId(), result.getResults());
            executedFunctions.add(current);

            current = graph.next(current, BranchType.DEFAULT);
        }

        if(failureCnt == 0) {
            return ResponseMessage.createSuccessMessage(createResponseData(executedFunctions, messageStorage));
        }
        else if(failureCnt < executedFunctions.size()) {
            return ResponseMessage.createErrorMessage(500, "E-EXE-002", "에러가 발생되었습니다.", messageStorage);
        }
        else {
            return ResponseMessage.createErrorMessage(500, "E-EXE-001", "에러가 발생되었습니다.", messageStorage);
        }
    }

    // 조건은 if, else if ... 순서대로 판정해서 처음 참이 된 가지로 흐른다. 전부 거짓이면 else 가지로 흐른다.
    private WorkflowNode nextOfCondition(
            WorkflowGraph graph,
            WorkflowNode condition,
            List<Map<String, Object>> params
    ) throws BusinessException {
        for(WorkflowCondition workflowCondition : graph.conditionsOf(condition)) {
            if(evaluate(workflowCondition.getConditionExpression(), params)) {
                return graph.nextCase(condition, workflowCondition.getBranchId());
            }
        }

        return graph.nextElse(condition);
    }

    private ResponseMessage errorResponseOf(
            WorkflowGraph graph,
            WorkflowNode node,
            Map<String, List<Map<String, Object>>> messageStorage
    ) throws BusinessException {
        WorkflowErrorResponse errorResponse = graph.errorResponseOf(node);
        if(errorResponse == null) {
            throw new BusinessException(WorkflowErrorMessage.NOT_SETTING_ERROR_RESPONSE);
        }

        return errorResponse.toResponseMessage(messageStorage);
    }

    private boolean evaluate(String conditionExpression, List<Map<String, Object>> params) throws BusinessException {
        try {
            return conditionEvaluator.evaluate(conditionExpression, params);
        }
        catch (ScriptEngineExecuteException e) {
            throw new BusinessException(WorkflowErrorMessage.FAILURE_CONDITION_EVALUATE);
        }
    }

    private static Map<String, List<Map<String, Object>>> createResponseData(
            List<WorkflowNode> functions,
            Map<String, List<Map<String, Object>>> messageStorage
    ) {
        Map<String, List<Map<String, Object>>> responseData = new HashMap<>();
        for(WorkflowNode func : functions) {
            responseData.put(func.getResponseMessageId(), messageStorage.get(func.getResponseMessageId()));
        }

        return responseData;
    }
}

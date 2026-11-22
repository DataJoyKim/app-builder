package com.prometis.appbuilder.app.workflow;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.dto.ResponseMessage;
import com.prometis.appbuilder.app.executor.script.ScriptEngineExecuteException;
import com.prometis.appbuilder.app.node.*;
import com.prometis.appbuilder.app.node.code.ResultType;
import com.prometis.appbuilder.app.workflow.code.BranchType;
import com.prometis.appbuilder.app.workflow.log.WorkflowExecutionLogService;
import com.prometis.appbuilder.platform.application.ApplicationGuard;
import com.prometis.appbuilder.platform.application.ApplicationNotFoundException;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(WorkflowProperties.class)
public class WorkflowService {
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final WorkflowEdgeRepository workflowEdgeRepository;
    private final WorkflowConditionRepository workflowConditionRepository;
    private final WorkflowErrorResponseRepository workflowErrorResponseRepository;
    private final NodeExecutorFactory nodeExecutorFactory;
    private final ConditionEvaluator conditionEvaluator;
    private final WorkflowGuard workflowGuard;
    private final ApplicationGuard applicationGuard;
    // 최대 실행 노드 수, 워크플로우 실행 노드의 최대 호출 깊이 (platform.workflow)
    private final WorkflowProperties workflowProperties;
    private final WorkflowExecutionLogService workflowExecutionLogService;

    /**
     * 워크플로우를 실행하고, 결과(성공/실패/거부)와 상관없이 실행 로그를 남긴다.
     * 없는 애플리케이션으로 온 요청만 남기지 않는다(로그의 애플리케이션이 실제로 있어야 하므로).
     */
    public ResponseMessage execute(
            HttpServletRequest request,
            HttpServletResponse response,
            String applicationId,
            RequestMessage requestMessage
    ) {
        LocalDateTime executedAt = LocalDateTime.now();
        long startedNanos = System.nanoTime();
        AuthenticatedUser user = null;
        ResponseMessage result;

        try {
            applicationGuard.check(applicationId);
        }
        catch (ApplicationNotFoundException e) {
            WorkflowErrorMessage errorCode = WorkflowErrorMessage.NOT_FOUND_APP;
            return ResponseMessage.createErrorMessage(errorCode.getStatus(), errorCode.getCode(), errorCode.getMsg());
        }

        try {
            Workflow workflow = findWorkflow(applicationId, requestMessage);
            requestMessage.getHeader().setApplicationId(applicationId);

            user = workflowGuard.check(request, applicationId, workflow);

            List<String> callChain = new ArrayList<>(List.of(workflow.getWorkflowCode()));

            result = executeFunction(requestMessage, user, createGraph(workflow), callChain);
        }
        catch (BusinessException e) {
            result = ResponseMessage.createErrorMessage(e.getStatus(), e.getCode(), e.getMsg());
        }
        catch (RuntimeException e) {
            // 예상하지 못한 예외는 그대로 올려 보내되, 실패한 실행으로 로그는 남긴다.
            ResponseMessage failed = ResponseMessage.createErrorMessage(500, "E-EXCEPTION", e.getClass().getSimpleName() + ": " + e.getMessage());
            recordExecution(request, applicationId, requestMessage, user, failed, executedAt, startedNanos);
            throw e;
        }

        recordExecution(request, applicationId, requestMessage, user, result, executedAt, startedNanos);
        return result;
    }

    private void recordExecution(
            HttpServletRequest request,
            String applicationId,
            RequestMessage requestMessage,
            AuthenticatedUser user,
            ResponseMessage result,
            LocalDateTime executedAt,
            long startedNanos
    ) {
        long durationMs = (System.nanoTime() - startedNanos) / 1_000_000;
        workflowExecutionLogService.record(request, applicationId, requestMessage, user, result, executedAt, durationMs);
    }

    private Workflow findWorkflow(String applicationId, RequestMessage requestMessage) throws BusinessException {
        Optional<Workflow> opWorkflow = workflowRepository.findByApplicationIdAndWorkflowCode(applicationId, requestMessage.getHeader().getWorkflowCode());
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
    // callChain: 지금 실행중인 워크플로우까지의 호출 경로. 워크플로우 실행 노드의 순환 호출을 막는 데 쓴다.
    private ResponseMessage executeFunction(
            RequestMessage requestMessage,
            AuthenticatedUser user,
            WorkflowGraph graph,
            List<String> callChain
    ) throws BusinessException {
        Map<String, List<Map<String, Object>>> messageStorage = requestMessage.getBody();
        if(messageStorage == null) {
            messageStorage = new HashMap<>();
            requestMessage.setBody(messageStorage);
        }

        // 응답에 담을 메시지ID. 노드의 응답메시지ID와, 워크플로우 실행 노드가 받아온 하위 워크플로우의 응답 메시지ID 들이다.
        Set<String> responseMessageIds = new LinkedHashSet<>();
        int executedCnt = 0;
        int failureCnt = 0;
        int step = 0;

        WorkflowNode current = graph.getStartNode();

        while(current != null) {
            if(++step > workflowProperties.maxExecuteStep()) {
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

            if(current.isWorkflowCall()) {
                ResponseMessage callResponse = callWorkflow(requestMessage, user, current, messageStorage, callChain);

                // 실행한 워크플로우가 실패하면(에러메시지 노드 포함) 그 에러응답으로 이 워크플로우도 끝낸다.
                if(!com.prometis.appbuilder.app.dto.ResultType.SUCCESS.equals(callResponse.getResultType())) {
                    return callResponse;
                }

                // 하위 워크플로우의 응답 메시지를 같은 메시지ID로 메시지 저장소에 담아 뒤의 노드가 이어서 쓴다.
                if(callResponse.getContents() != null) {
                    messageStorage.putAll(callResponse.getContents());
                    responseMessageIds.addAll(callResponse.getContents().keySet());
                }
                executedCnt++;

                current = graph.next(current, BranchType.DEFAULT);
                continue;
            }

            NodeExecutor executor = nodeExecutorFactory.instance(current.getFunctionType());

            NodeResult result = executor.execute(user, current.getFunctionName(), requestMessage.getHeader(), params);

            if(ResultType.FAILURE.equals(result.getResultType())) {
                failureCnt++;
            }

            messageStorage.put(current.getResponseMessageId(), result.getResults());
            responseMessageIds.add(current.getResponseMessageId());
            executedCnt++;

            current = graph.next(current, BranchType.DEFAULT);
        }

        if(failureCnt == 0) {
            return ResponseMessage.createSuccessMessage(createResponseData(responseMessageIds, messageStorage));
        }
        else if(failureCnt < executedCnt) {
            return ResponseMessage.createErrorMessage(500, "E-EXE-002", "에러가 발생되었습니다.", messageStorage);
        }
        else {
            return ResponseMessage.createErrorMessage(500, "E-EXE-001", "에러가 발생되었습니다.", messageStorage);
        }
    }

    /**
     * 워크플로우 실행(WORKFLOW) 노드. 기능코드(functionName)의 워크플로우를 같은 애플리케이션에서 찾아 이어서 실행한다.
     * 하위 워크플로우는 지금까지의 메시지 저장소 사본을 요청 본문으로 받으므로, 자기가 읽는 메시지ID 를 그대로 찾는다.
     * 호출한 워크플로우 안에서의 실행이라 인증, 권한, IP, 도메인 검사는 처음 호출된 워크플로우에서 한 번만 한다.
     * 콘솔 저장에서 순환 호출을 막지만, 저장 뒤에 워크플로우가 바뀌는 경우를 위해 실행할 때도 호출 경로로 한 번 더 막는다.
     */
    private ResponseMessage callWorkflow(
            RequestMessage requestMessage,
            AuthenticatedUser user,
            WorkflowNode node,
            Map<String, List<Map<String, Object>>> messageStorage,
            List<String> callChain
    ) throws BusinessException {
        String targetCode = node.getFunctionName();
        if(callChain.contains(targetCode)) {
            throw new BusinessException(WorkflowErrorMessage.CIRCULAR_WORKFLOW_CALL);
        }
        if(callChain.size() >= workflowProperties.maxCallDepth()) {
            throw new BusinessException(WorkflowErrorMessage.EXCEED_MAX_CALL_DEPTH);
        }

        String applicationId = requestMessage.getHeader().getApplicationId();
        Workflow target = workflowRepository.findByApplicationIdAndWorkflowCode(applicationId, targetCode)
                .orElseThrow(() -> new BusinessException(WorkflowErrorMessage.NOT_FOUND_CALL_WORKFLOW));

        RequestMessage.Header header = new RequestMessage.Header();
        header.setWorkflowCode(targetCode);
        header.setObjectCode(requestMessage.getHeader().getObjectCode());
        header.setLocaleCode(requestMessage.getHeader().getLocaleCode());
        header.setApplicationId(applicationId);
        header.setFiles(requestMessage.getHeader().getFiles());

        RequestMessage callRequest = new RequestMessage();
        callRequest.setHeader(header);
        callRequest.setBody(new HashMap<>(messageStorage));

        List<String> nextChain = new ArrayList<>(callChain);
        nextChain.add(targetCode);

        return executeFunction(callRequest, user, createGraph(target), nextChain);
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
            Set<String> responseMessageIds,
            Map<String, List<Map<String, Object>>> messageStorage
    ) {
        Map<String, List<Map<String, Object>>> responseData = new HashMap<>();
        for(String responseMessageId : responseMessageIds) {
            responseData.put(responseMessageId, messageStorage.get(responseMessageId));
        }

        return responseData;
    }
}

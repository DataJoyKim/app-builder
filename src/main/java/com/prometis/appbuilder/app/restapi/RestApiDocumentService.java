package com.prometis.appbuilder.app.restapi;

import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.node.WorkflowNodeRepository;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowErrorResponse;
import com.prometis.appbuilder.app.workflow.WorkflowErrorResponseRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 콘솔의 API 문서 화면에 쓸 정보를 모은다.
 * API 정의와 파라미터 스키마에 더해, 감싼 워크플로우에서 응답에 담길 수 있는 메시지ID 와
 * 에러메시지 노드가 돌려줄 수 있는 업무 오류까지 같이 보여줘야 호출하는 쪽이 응답을 예상할 수 있다.
 */
@Service
@RequiredArgsConstructor
public class RestApiDocumentService {
    private final RestApiRepository restApiRepository;
    private final RestApiParameterRepository restApiParameterRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final WorkflowErrorResponseRepository workflowErrorResponseRepository;

    public Map<String, Object> document() {
        List<RestApi> restApis = new ArrayList<>(restApiRepository.findAll());
        restApis.sort(Comparator
                .comparing(RestApi::getPath, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(api -> api.getHttpMethod() == null ? 0 : api.getHttpMethod().ordinal()));

        List<Map<String, Object>> apis = new ArrayList<>();
        for(RestApi restApi : restApis) {
            apis.add(documentOf(restApi));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("urlPrefix", RestApi.URL_PREFIX);
        result.put("apis", apis);
        result.put("commonErrors", commonErrors());

        return result;
    }

    private Map<String, Object> documentOf(RestApi restApi) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("restApi", restApi);
        document.put("parameters", restApiParameterRepository.findByRestApiIdOrderByOrderNum(restApi.getId()));

        Optional<Workflow> workflow = workflowRepository.findByWorkflowCode(restApi.getWorkflowCode());
        if(workflow.isEmpty()) {
            document.put("workflow", null);
            document.put("responseMessageIds", List.of());
            document.put("errorResponses", List.of());
            return document;
        }

        Map<String, Object> workflowInfo = new LinkedHashMap<>();
        workflowInfo.put("workflowCode", workflow.get().getWorkflowCode());
        workflowInfo.put("displayName", workflow.get().getDisplayName());
        workflowInfo.put("useAuthValidation", workflow.get().getUseAuthValidation());
        document.put("workflow", workflowInfo);

        document.put("responseMessageIds", responseMessageIdsOf(workflow.get()));

        List<Map<String, Object>> errorResponses = new ArrayList<>();
        for(WorkflowErrorResponse errorResponse : workflowErrorResponseRepository.findByWorkflowId(workflow.get().getId())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", WorkflowErrorResponse.resolveStatus(errorResponse.getStatus()));
            error.put("code", errorResponse.getCode());
            error.put("message", errorResponse.getMessage());
            error.put("contents", errorResponse.getContents());
            errorResponses.add(error);
        }
        document.put("errorResponses", errorResponses);

        return document;
    }

    /**
     * 워크플로우가 성공 응답의 contents 에 담을 수 있는 응답메시지ID. 콘솔의 응답 설정에서 고를 수 있게 보여준다.
     */
    public List<String> responseMessageIdsOf(String workflowCode) {
        return workflowRepository.findByWorkflowCode(workflowCode)
                .map(this::responseMessageIdsOf)
                .orElse(List.of());
    }

    // 성공 응답의 contents 는 실행된 노드들의 응답메시지ID 로 채워진다. 조건분기/에러메시지 노드는 응답을 만들지 않는다.
    private List<String> responseMessageIdsOf(Workflow workflow) {
        LinkedHashSet<String> responseMessageIds = new LinkedHashSet<>();
        List<WorkflowNode> nodes = new ArrayList<>(workflowNodeRepository.findByWorkflowId(workflow.getId()));
        nodes.sort(Comparator.comparing(WorkflowNode::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));

        for(WorkflowNode node : nodes) {
            if(node.isCondition() || node.isErrorMessage()) {
                continue;
            }

            if(node.getResponseMessageId() != null && !node.getResponseMessageId().isBlank()) {
                responseMessageIds.add(node.getResponseMessageId());
            }
        }

        return new ArrayList<>(responseMessageIds);
    }

    // 모든 API 가 공통으로 돌려줄 수 있는 오류.
    private static List<Map<String, Object>> commonErrors() {
        List<Map<String, Object>> errors = new ArrayList<>();

        errors.add(error(RestApiValidationException.STATUS, RestApiValidationException.REQUEST_CODE,
                "요청값이 스키마에 맞지 않습니다. message 에 어느 값이 왜 틀렸는지 담깁니다."));

        for(RestApiErrorMessage message : RestApiErrorMessage.values()) {
            errors.add(error(message.getStatus(), message.getCode(), message.getMsg()));
        }

        errors.add(error(RestApiValidationException.RESPONSE_STATUS, RestApiValidationException.RESPONSE_CODE,
                "워크플로우 결과가 응답스키마에 맞지 않습니다. message 에 어느 값이 왜 틀렸는지 담깁니다."));
        errors.add(error(500, "E-EXE-001", "워크플로우의 모든 단계가 실패하였습니다."));
        errors.add(error(500, "E-EXE-002", "워크플로우의 일부 단계가 실패하였습니다. contents 에 그때까지의 결과가 담깁니다."));

        errors.sort(Comparator.comparing(e -> (Integer) e.get("status")));

        return errors;
    }

    private static Map<String, Object> error(Integer status, String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", status);
        error.put("code", code);
        error.put("message", message);
        return error;
    }
}

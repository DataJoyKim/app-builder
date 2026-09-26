package com.prometis.appbuilder.app.restapi;

import com.prometis.appbuilder.app.node.WorkflowNode;
import com.prometis.appbuilder.app.node.WorkflowNodeRepository;
import com.prometis.appbuilder.app.node.code.FunctionType;
import com.prometis.appbuilder.app.restapi.*;
import com.prometis.appbuilder.app.restapi.code.HttpMethodType;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowErrorResponse;
import com.prometis.appbuilder.app.workflow.WorkflowErrorResponseRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(RestApiDocumentService.class)
class RestApiDocumentServiceTest {
    @Autowired
    private RestApiDocumentService restApiDocumentService;
    @Autowired
    private RestApiRepository restApiRepository;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private WorkflowNodeRepository workflowNodeRepository;
    @Autowired
    private WorkflowErrorResponseRepository workflowErrorResponseRepository;

    private WorkflowNode node(Long workflowId, String nodeId, FunctionType functionType, int orderNum, String responseMessageId) {
        return WorkflowNode.builder()
                .workflowId(workflowId)
                .nodeId(nodeId)
                .functionName(nodeId)
                .functionType(functionType)
                .orderNum(orderNum)
                .responseMessageId(responseMessageId)
                .build();
    }

    private RestApi restApi(String apiCode, HttpMethodType method, String path, String workflowCode) {
        return RestApi.builder()
                .apiCode(apiCode)
                .displayName(apiCode)
                .httpMethod(method)
                .path(path)
                .workflowCode(workflowCode)
                .enabled(true)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void 문서에는_워크플로우의_응답메시지와_업무오류가_함께_담긴다() {
        Workflow workflow = workflowRepository.save(Workflow.builder()
                .workflowCode("WF_DOC")
                .displayName("문서 워크플로우")
                .useAuthValidation(true)
                .build());

        workflowNodeRepository.save(node(workflow.getId(), "n1", FunctionType.SQL, 1, "ME_LIST"));
        // 조건분기/에러메시지 노드는 응답을 만들지 않으므로 응답메시지에서 빠진다.
        workflowNodeRepository.save(node(workflow.getId(), "n2", FunctionType.CONDITION, 2, "ME_IGNORED"));
        workflowNodeRepository.save(node(workflow.getId(), "n3", FunctionType.ERROR_MESSAGE, 3, "ME_IGNORED2"));
        workflowNodeRepository.save(node(workflow.getId(), "n4", FunctionType.ENTITY, 4, "ME_SAVE"));
        workflowNodeRepository.save(node(workflow.getId(), "n5", FunctionType.SQL, 5, "ME_LIST"));

        workflowErrorResponseRepository.save(WorkflowErrorResponse.builder()
                .workflowId(workflow.getId())
                .nodeId("n3")
                .status(200)
                .code("E-DOC-001")
                .message("저장할 수 없습니다.")
                .build());

        restApiRepository.save(restApi("DOC_SAVE", HttpMethodType.POST, "/docs", "WF_DOC"));
        restApiRepository.save(restApi("DOC_LIST", HttpMethodType.GET, "/docs", "WF_DOC"));
        restApiRepository.save(restApi("DOC_MISSING", HttpMethodType.GET, "/a-missing", "WF_NOT_EXISTS"));

        Map<String, Object> document = restApiDocumentService.document();
        assertEquals(RestApi.URL_PREFIX, document.get("urlPrefix"));

        List<Map<String, Object>> apis = (List<Map<String, Object>>) document.get("apis");

        // 경로 순, 같은 경로는 GET/POST/PUT/DELETE 순으로 정렬된다.
        assertEquals(List.of("DOC_MISSING", "DOC_LIST", "DOC_SAVE"),
                apis.stream().map(api -> ((RestApi) api.get("restApi")).getApiCode()).toList());

        Map<String, Object> list = apis.get(1);
        assertEquals(List.of("ME_LIST", "ME_SAVE"), list.get("responseMessageIds"));
        assertEquals(true, ((Map<String, Object>) list.get("workflow")).get("useAuthValidation"));

        List<Map<String, Object>> errorResponses = (List<Map<String, Object>>) list.get("errorResponses");
        assertEquals(1, errorResponses.size());
        // 실제 응답과 같게, 에러 상태가 아닌 값은 400 으로 보여준다.
        assertEquals(400, errorResponses.get(0).get("status"));
        assertEquals("E-DOC-001", errorResponses.get(0).get("code"));

        // 워크플로우를 찾을 수 없어도 문서는 만들어진다.
        Map<String, Object> missing = apis.get(0);
        assertNull(missing.get("workflow"));
        assertEquals(List.of(), missing.get("responseMessageIds"));

        List<Map<String, Object>> commonErrors = (List<Map<String, Object>>) document.get("commonErrors");
        assertTrue(commonErrors.stream().anyMatch(e -> RestApiValidationException.REQUEST_CODE.equals(e.get("code"))));
        assertTrue(commonErrors.stream().anyMatch(e -> RestApiErrorMessage.METHOD_NOT_ALLOWED.getCode().equals(e.get("code"))));
    }
}

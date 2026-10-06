package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowPermission;
import com.prometis.appbuilder.app.workflow.WorkflowPermissionRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 권한 화면에서 권한별로 워크플로우 실행 권한(WorkflowPermission)을 연결하는 API.
 * 콘솔 인증 필터는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow-permission;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class WorkflowPermissionRestControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    WorkflowRepository workflowRepository;
    @Autowired
    WorkflowPermissionRepository workflowPermissionRepository;

    private Workflow orderList;
    private Workflow orderSave;
    private Workflow crmWorkflow;

    @BeforeEach
    void setUp() {
        workflowPermissionRepository.deleteAll();
        workflowRepository.deleteAll();

        orderList = workflow("ehr", "ORDER_LIST", "주문 조회");
        orderSave = workflow("ehr", "ORDER_SAVE", "주문 저장");
        crmWorkflow = workflow("crm", "CRM_LIST", "고객 조회");
    }

    private Workflow workflow(String applicationId, String code, String name) {
        return workflowRepository.save(Workflow.builder()
                .applicationId(applicationId)
                .workflowCode(code)
                .displayName(name)
                .useAuthValidation(true)
                .build());
    }

    private ResultActions bind(String applicationId, Object workflowId, String permissionCode) throws Exception {
        String id = workflowId instanceof String ? "\"" + workflowId + "\"" : String.valueOf(workflowId);
        return mockMvc.perform(post("/" + applicationId + "/console/api/workflow-permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workflowId\":" + id + ",\"permissionCode\":\"" + permissionCode + "\"}"));
    }

    @Test
    void 권한에_워크플로우를_연결하고_권한별로_조회한다() throws Exception {
        // 화면에 따라 id 를 문자열 또는 숫자로 보낸다
        bind("ehr", String.valueOf(orderList.getId()), "ORDER_READ").andExpect(status().isOk());
        bind("ehr", orderSave.getId(), "ORDER_READ").andExpect(status().isOk());
        bind("ehr", orderSave.getId(), "ORDER_WRITE").andExpect(status().isOk());

        mockMvc.perform(get("/ehr/console/api/workflow-permission/by-permission").param("permissionCode", "ORDER_READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].workflow.workflowCode", containsInAnyOrder("ORDER_LIST", "ORDER_SAVE")));

        // 워크플로우 빌더가 쓰는 워크플로우별 조회에도 같은 연결이 보인다
        mockMvc.perform(get("/ehr/console/api/workflow-permission").param("workflowId", String.valueOf(orderSave.getId())))
                .andExpect(jsonPath("$[*].permissionCode", containsInAnyOrder("ORDER_READ", "ORDER_WRITE")));
    }

    @Test
    void 같은_워크플로우에_같은_권한을_두_번_연결하지_않는다() throws Exception {
        bind("ehr", orderList.getId(), "ORDER_READ").andExpect(status().isOk());

        bind("ehr", orderList.getId(), "ORDER_READ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("이미")));

        assertEquals(1, workflowPermissionRepository.findByWorkflow(orderList).size());
    }

    @Test
    void 다른_애플리케이션의_워크플로우는_권한별_조회에_나오지_않는다() throws Exception {
        workflowPermissionRepository.save(WorkflowPermission.builder().permissionCode("ORDER_READ").workflow(crmWorkflow).build());
        bind("ehr", orderList.getId(), "ORDER_READ").andExpect(status().isOk());

        mockMvc.perform(get("/ehr/console/api/workflow-permission/by-permission").param("permissionCode", "ORDER_READ"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].workflow.workflowCode").value("ORDER_LIST"));
    }

    @Test
    void 연결을_해제한다() throws Exception {
        bind("ehr", orderList.getId(), "ORDER_READ").andExpect(status().isOk());
        WorkflowPermission saved = workflowPermissionRepository.findByWorkflow(orderList).get(0);

        mockMvc.perform(delete("/ehr/console/api/workflow-permission/" + saved.getId()))
                .andExpect(status().isOk());

        assertTrue(workflowPermissionRepository.findByWorkflow(orderList).isEmpty());
    }
}

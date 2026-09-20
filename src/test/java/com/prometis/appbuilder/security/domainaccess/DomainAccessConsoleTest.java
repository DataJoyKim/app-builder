package com.prometis.appbuilder.security.domainaccess;

import com.prometis.appbuilder.workflow.Workflow;
import com.prometis.appbuilder.workflow.WorkflowDomainRepository;
import com.prometis.appbuilder.workflow.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 워크플로우 빌더에서 저장한 허용 도메인이 실행 시 Referer 검증에 쓰이는지 확인한다.
 * 콘솔 인증 필터(ConsoleSecurityFilter)는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:domain-access;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class DomainAccessConsoleTest {
    private static final String WORKFLOW_CODE = "WF_DOMAIN_ACCESS";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    WorkflowRepository workflowRepository;
    @Autowired
    WorkflowDomainRepository workflowDomainRepository;

    @BeforeEach
    void setUp() {
        workflowDomainRepository.deleteAll();
        workflowRepository.deleteAll();
    }

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MvcResult saveWorkflowWithDomains(String... domains) throws Exception {
        String domainJson = String.join(",", List.of(domains).stream()
                .map(domain -> "{\"domain\":\"" + domain + "\"}")
                .toList());

        String workflowId = workflowRepository.findByWorkflowCode(WORKFLOW_CODE)
                .map(workflow -> String.valueOf(workflow.getId()))
                .orElse("");

        return mockMvc.perform(post("/console/api/workflow/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow\":{\"id\":\"" + workflowId + "\",\"workflowCode\":\"" + WORKFLOW_CODE + "\",\"displayName\":\"도메인 테스트\",\"note\":\"\",\"useAuthValidation\":false},"
                                + "\"workflowNodes\":[],\"workflowEdges\":[],\"workflowConditions\":[],\"workflowErrorResponses\":[],"
                                + "\"workflowAuthority\":[],\"workflowIpGroup\":[],\"workflowDomain\":[" + domainJson + "]}"))
                .andReturn();
    }

    private MvcResult executeWorkflow(String referer) throws Exception {
        var request = post("/workflow")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"header\":{\"workflowCode\":\"" + WORKFLOW_CODE + "\"},\"body\":{}}");

        if(referer != null) {
            request = request.header(HttpHeaders.REFERER, referer);
        }

        return mockMvc.perform(request).andReturn();
    }

    @Test
    void 저장한_도메인이_워크플로우에_매핑되고_다시_조회된다() throws Exception {
        saveWorkflowWithDomains("portal.example.com", "*.partner.com");

        Workflow workflow = workflowRepository.findByWorkflowCode(WORKFLOW_CODE).orElseThrow();

        String list = body(mockMvc.perform(get("/console/api/workflow-domain").param("workflowId", String.valueOf(workflow.getId())))
                .andExpect(status().isOk())
                .andReturn());

        assertTrue(list.contains("portal.example.com"), list);
        assertTrue(list.contains("*.partner.com"), list);

        // 저장할 때마다 통째로 교체한다.
        saveWorkflowWithDomains("portal.example.com");
        assertEquals(1, workflowDomainRepository.findByWorkflow(workflow).size());
    }

    @Test
    void 도메인_형식이_틀리면_저장하지않는다() throws Exception {
        MvcResult result = saveWorkflowWithDomains("https://example.com/board");

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(body(result).contains("도메인 형식이 올바르지않습니다"), body(result));
        // 형식 검사는 저장 전에 하므로 워크플로우 자체도 저장되지 않는다.
        assertTrue(workflowRepository.findByWorkflowCode(WORKFLOW_CODE).isEmpty());
    }

    @Test
    void 허용_도메인의_Referer로만_실행된다() throws Exception {
        saveWorkflowWithDomains("portal.example.com", "*.partner.com");

        MvcResult allowed = executeWorkflow("https://portal.example.com/board/list");
        assertEquals(200, allowed.getResponse().getStatus());
        assertTrue(body(allowed).contains("\"resultType\":\"SUCCESS\""), body(allowed));

        MvcResult wildcard = executeWorkflow("https://shop.partner.com:8443/order");
        assertEquals(200, wildcard.getResponse().getStatus());

        MvcResult denied = executeWorkflow("https://attacker.com/page");
        assertEquals(403, denied.getResponse().getStatus());
        assertTrue(body(denied).contains("E-WORKFLOW-012"), body(denied));

        MvcResult noReferer = executeWorkflow(null);
        assertEquals(403, noReferer.getResponse().getStatus());
    }

    @Test
    void 도메인을_등록하지않은_워크플로우는_제한없이_실행된다() throws Exception {
        saveWorkflowWithDomains();

        MvcResult result = executeWorkflow(null);

        assertEquals(200, result.getResponse().getStatus());
        assertTrue(body(result).contains("\"resultType\":\"SUCCESS\""), body(result));
    }
}

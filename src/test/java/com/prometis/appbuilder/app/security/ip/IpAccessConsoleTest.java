package com.prometis.appbuilder.app.security.ip;

import com.prometis.appbuilder.app.security.ip.IpAddressRepository;
import com.prometis.appbuilder.app.security.ip.IpGroupRepository;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroupRepository;
import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IP 관리 화면/API 와 워크플로우 IP 접근제어를 콘솔 저장부터 실행까지 확인한다.
 * 콘솔 인증 필터(ConsoleSecurityFilter)는 addFilters=false 로 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ip-access;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.quartz.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = false)
class IpAccessConsoleTest {
    private static final String WORKFLOW_CODE = "WF_IP_ACCESS";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    IpGroupRepository ipGroupRepository;
    @Autowired
    IpAddressRepository ipAddressRepository;
    @Autowired
    WorkflowRepository workflowRepository;
    @Autowired
    WorkflowIpGroupRepository workflowIpGroupRepository;

    @BeforeEach
    void setUp() {
        workflowIpGroupRepository.deleteAll();
        workflowRepository.deleteAll();
        ipAddressRepository.deleteAll();
        ipGroupRepository.deleteAll();
    }

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Long createGroup(String groupCode, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/console/api/ip-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupCode\":\"" + groupCode + "\",\"displayName\":\"" + displayName + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return ipGroupRepository.findByGroupCode(groupCode).orElseThrow().getId();
    }

    private void saveAddresses(Long groupId, String json) throws Exception {
        mockMvc.perform(put("/console/api/ip-group/" + groupId + "/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    private void saveWorkflowWithIpGroup(String ipGroupCode) throws Exception {
        String ipGroups = (ipGroupCode == null) ? "" : "{\"ipGroupCode\":\"" + ipGroupCode + "\"}";

        // 이미 저장해둔 워크플로우면 그 id 로 수정한다. (화면의 저장 버튼과 같은 흐름)
        String workflowId = workflowRepository.findByWorkflowCode(WORKFLOW_CODE)
                .map(workflow -> String.valueOf(workflow.getId()))
                .orElse("");

        mockMvc.perform(post("/console/api/workflow/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow\":{\"id\":\"" + workflowId + "\",\"workflowCode\":\"" + WORKFLOW_CODE + "\",\"displayName\":\"IP 테스트\",\"note\":\"\",\"useAuthValidation\":false},"
                                + "\"workflowNodes\":[],\"workflowEdges\":[],\"workflowConditions\":[],\"workflowErrorResponses\":[],"
                                + "\"workflowAuthority\":[],\"workflowIpGroup\":[" + ipGroups + "]}"))
                .andExpect(status().isOk());
    }

    private MvcResult executeWorkflow(String clientIp) throws Exception {
        return mockMvc.perform(post("/workflow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"header\":{\"workflowCode\":\"" + WORKFLOW_CODE + "\"},\"body\":{}}")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andReturn();
    }

    @Test
    void IP관리_화면이_열린다() throws Exception {
        MvcResult result = mockMvc.perform(get("/console/ip-management"))
                .andExpect(status().isOk())
                .andReturn();

        String html = body(result);

        assertTrue(html.contains("IP그룹 목록"), html.substring(0, Math.min(500, html.length())));
        assertTrue(html.contains("/console/api/ip-group"));
    }

    @Test
    void IP그룹과_허용IP를_등록하고_목록에_개수가_보인다() throws Exception {
        Long groupId = createGroup("OFFICE", "사내망");

        saveAddresses(groupId, "[{\"ipAddress\":\"192.168.0.0/24\",\"note\":\"본사\",\"enabled\":true},"
                + "{\"ipAddress\":\"10.0.0.5\",\"note\":\"VPN\",\"enabled\":false}]");

        String list = body(mockMvc.perform(get("/console/api/ip-group")).andExpect(status().isOk()).andReturn());
        assertTrue(list.contains("\"ipCount\":2"), list);

        String detail = body(mockMvc.perform(get("/console/api/ip-group/" + groupId)).andExpect(status().isOk()).andReturn());
        assertTrue(detail.contains("192.168.0.0/24"), detail);
        assertTrue(detail.contains("\"enabled\":false"), detail);

        // 저장할 때마다 통째로 교체한다.
        saveAddresses(groupId, "[{\"ipAddress\":\"172.16.0.1\"}]");
        assertEquals(1, ipAddressRepository.findByIpGroupIdOrderByOrderNumAsc(groupId).size());
    }

    @Test
    void 중복코드와_잘못된_IP형식은_거부한다() throws Exception {
        Long groupId = createGroup("OFFICE", "사내망");

        mockMvc.perform(post("/console/api/ip-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupCode\":\"OFFICE\",\"displayName\":\"중복\"}"))
                .andExpect(status().isBadRequest());

        MvcResult invalid = mockMvc.perform(put("/console/api/ip-group/" + groupId + "/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"ipAddress\":\"사내망\"}]"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(body(invalid).contains("IP 형식이 올바르지않습니다"), body(invalid));
        assertTrue(ipAddressRepository.findByIpGroupIdOrderByOrderNumAsc(groupId).isEmpty());
    }

    @Test
    void 워크플로우에_사용중인_IP그룹은_삭제할수없다() throws Exception {
        Long groupId = createGroup("OFFICE", "사내망");
        saveWorkflowWithIpGroup("OFFICE");

        MvcResult denied = mockMvc.perform(delete("/console/api/ip-group/" + groupId))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(body(denied).contains(WORKFLOW_CODE), body(denied));

        // 매핑을 풀면 삭제할 수 있다.
        saveWorkflowWithIpGroup(null);
        mockMvc.perform(delete("/console/api/ip-group/" + groupId)).andExpect(status().isOk());
    }

    @Test
    void 그룹코드를_바꾸면_워크플로우_매핑도_따라간다() throws Exception {
        Long groupId = createGroup("OFFICE", "사내망");
        saveWorkflowWithIpGroup("OFFICE");

        mockMvc.perform(put("/console/api/ip-group/" + groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupCode\":\"HQ\",\"displayName\":\"본사망\"}"))
                .andExpect(status().isOk());

        Workflow workflow = workflowRepository.findByWorkflowCode(WORKFLOW_CODE).orElseThrow();
        assertEquals("HQ", workflowIpGroupRepository.findByWorkflow(workflow).get(0).getIpGroupCode());
    }

    @Test
    void 매핑된_IP그룹의_허용IP에서만_워크플로우가_실행된다() throws Exception {
        Long groupId = createGroup("OFFICE", "사내망");
        saveAddresses(groupId, "[{\"ipAddress\":\"192.168.0.0/24\"}]");
        saveWorkflowWithIpGroup("OFFICE");

        MvcResult denied = executeWorkflow("10.0.0.1");
        assertEquals(403, denied.getResponse().getStatus());
        assertTrue(body(denied).contains("E-WORKFLOW-011"), body(denied));

        MvcResult allowed = executeWorkflow("192.168.0.50");
        assertEquals(200, allowed.getResponse().getStatus());
        assertTrue(body(allowed).contains("\"resultType\":\"SUCCESS\""), body(allowed));
    }

    @Test
    void IP그룹을_매핑하지않은_워크플로우는_IP제한없이_실행된다() throws Exception {
        saveWorkflowWithIpGroup(null);

        MvcResult result = executeWorkflow("203.0.113.1");

        assertEquals(200, result.getResponse().getStatus());
        assertTrue(body(result).contains("\"resultType\":\"SUCCESS\""), body(result));
    }
}

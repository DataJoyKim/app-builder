package com.prometis.appbuilder.app.security.ip;

import com.prometis.appbuilder.app.security.ip.*;
import com.prometis.appbuilder.app.workflow.Workflow;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroup;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroupRepository;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 워크플로우에 매핑된 IP 그룹으로 접속 IP 를 검증하는지 확인한다.
 */
class IpAccessValidatorTest {
    private static final String GROUP_CODE = "OFFICE";
    private static final Long GROUP_ID = 1L;

    private WorkflowIpGroupRepository workflowIpGroupRepository;
    private IpGroupRepository ipGroupRepository;
    private IpAddressRepository ipAddressRepository;
    private IpAccessValidator validator;

    private final Workflow workflow = Workflow.builder()
            .id(10L)
            .workflowCode("WF01")
            .displayName("테스트")
            .useAuthValidation(false)
            .build();

    @BeforeEach
    void setUp() {
        workflowIpGroupRepository = mock(WorkflowIpGroupRepository.class);
        ipGroupRepository = mock(IpGroupRepository.class);
        ipAddressRepository = mock(IpAddressRepository.class);

        validator = new IpAccessValidator(workflowIpGroupRepository, ipGroupRepository, ipAddressRepository);
    }

    private void mapGroup() {
        when(workflowIpGroupRepository.findByWorkflow(workflow)).thenReturn(List.of(
                WorkflowIpGroup.builder().id(1L).ipGroupCode(GROUP_CODE).workflow(workflow).build()
        ));

        when(ipGroupRepository.findByGroupCodeIn(List.of(GROUP_CODE))).thenReturn(List.of(
                IpGroup.builder().id(GROUP_ID).groupCode(GROUP_CODE).displayName("사내망").build()
        ));
    }

    private void allowIps(IpAddress... ipAddresses) {
        when(ipAddressRepository.findByIpGroupIdIn(List.of(GROUP_ID))).thenReturn(List.of(ipAddresses));
    }

    private static IpAddress ip(String ipAddress, boolean enabled) {
        return IpAddress.builder()
                .id(1L)
                .ipGroupId(GROUP_ID)
                .ipAddress(ipAddress)
                .enabled(enabled)
                .build();
    }

    private static HttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);

        return request;
    }

    @Test
    void 매핑된_IP그룹이_없으면_제한하지않는다() throws Exception {
        when(workflowIpGroupRepository.findByWorkflow(workflow)).thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validate(request("1.2.3.4"), workflow));

        verify(ipAddressRepository, never()).findByIpGroupIdIn(anyList());
    }

    @Test
    void 허용_IP면_통과한다() throws Exception {
        mapGroup();
        allowIps(ip("192.168.0.0/24", true));

        assertDoesNotThrow(() -> validator.validate(request("192.168.0.77"), workflow));
    }

    @Test
    void 허용되지않은_IP면_막는다() {
        mapGroup();
        allowIps(ip("192.168.0.0/24", true));

        BusinessException e = assertThrows(BusinessException.class, () -> validator.validate(request("10.0.0.1"), workflow));

        assertEquals("E-WORKFLOW-011", e.getCode());
        assertEquals(403, e.getStatus());
    }

    @Test
    void 사용안함으로_꺼둔_IP는_허용하지않는다() {
        mapGroup();
        allowIps(ip("10.0.0.1", false));

        assertThrows(BusinessException.class, () -> validator.validate(request("10.0.0.1"), workflow));
    }

    @Test
    void 허용_IP가_하나도_없으면_막는다() {
        mapGroup();
        allowIps();

        assertThrows(BusinessException.class, () -> validator.validate(request("10.0.0.1"), workflow));
    }

    @Test
    void 프록시를_거치면_XForwardedFor의_첫_IP로_검증한다() throws Exception {
        mapGroup();
        allowIps(ip("203.0.113.5", true));

        MockHttpServletRequest allowed = new MockHttpServletRequest();
        allowed.setRemoteAddr("10.0.0.9");
        allowed.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.9");

        assertDoesNotThrow(() -> validator.validate(allowed, workflow));

        MockHttpServletRequest denied = new MockHttpServletRequest();
        denied.setRemoteAddr("203.0.113.5");
        denied.addHeader("X-Forwarded-For", "8.8.8.8, 203.0.113.5");

        assertThrows(BusinessException.class, () -> validator.validate(denied, workflow));
    }

    @Test
    void 요청이_없는_실행은_IP제한을_적용하지않는다() throws Exception {
        mapGroup();
        allowIps(ip("192.168.0.1", true));

        assertDoesNotThrow(() -> validator.validate(null, workflow));
    }

    @Test
    void 매핑된_그룹이_삭제되어_없으면_막는다() {
        when(workflowIpGroupRepository.findByWorkflow(workflow)).thenReturn(List.of(
                WorkflowIpGroup.builder().id(1L).ipGroupCode(GROUP_CODE).workflow(workflow).build()
        ));
        when(ipGroupRepository.findByGroupCodeIn(any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> validator.validate(request("192.168.0.1"), workflow));
    }
}

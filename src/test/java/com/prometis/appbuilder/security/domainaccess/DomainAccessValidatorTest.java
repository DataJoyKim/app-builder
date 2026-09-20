package com.prometis.appbuilder.security.domainaccess;

import com.prometis.appbuilder.workflow.Workflow;
import com.prometis.appbuilder.workflow.WorkflowDomain;
import com.prometis.appbuilder.workflow.WorkflowDomainRepository;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 워크플로우에 매핑된 도메인으로 Referer 도메인을 검증하는지 확인한다.
 */
class DomainAccessValidatorTest {
    private WorkflowDomainRepository workflowDomainRepository;
    private DomainAccessValidator validator;

    private final Workflow workflow = Workflow.builder()
            .id(10L)
            .workflowCode("WF01")
            .displayName("테스트")
            .useAuthValidation(false)
            .build();

    @BeforeEach
    void setUp() {
        workflowDomainRepository = mock(WorkflowDomainRepository.class);
        validator = new DomainAccessValidator(workflowDomainRepository);
    }

    private void allowDomains(String... domains) {
        when(workflowDomainRepository.findByWorkflow(workflow)).thenReturn(
                Arrays.stream(domains)
                        .map(domain -> WorkflowDomain.builder().id(1L).domain(domain).workflow(workflow).build())
                        .toList()
        );
    }

    private static HttpServletRequest referer(String referer) {
        MockHttpServletRequest request = new MockHttpServletRequest();

        if(referer != null) {
            request.addHeader(DomainAccessValidator.REFERER_HEADER, referer);
        }

        return request;
    }

    @Test
    void 매핑된_도메인이_없으면_제한하지않는다() {
        when(workflowDomainRepository.findByWorkflow(workflow)).thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validate(referer(null), workflow));
    }

    @Test
    void 허용_도메인의_Referer면_통과한다() {
        allowDomains("portal.example.com");

        assertDoesNotThrow(() -> validator.validate(referer("https://portal.example.com/board/list?page=2"), workflow));
    }

    @Test
    void 허용되지않은_도메인이면_막는다() {
        allowDomains("portal.example.com");

        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(referer("https://attacker.com/page"), workflow));

        assertEquals("E-WORKFLOW-012", e.getCode());
        assertEquals(403, e.getStatus());
    }

    @Test
    void 와일드카드로_하위도메인을_허용한다() {
        allowDomains("*.example.com");

        assertDoesNotThrow(() -> validator.validate(referer("https://portal.example.com/x"), workflow));
        assertDoesNotThrow(() -> validator.validate(referer("https://a.b.example.com/x"), workflow));
        assertThrows(BusinessException.class, () -> validator.validate(referer("https://example.com/x"), workflow));
    }

    @Test
    void 여러_도메인중_하나만_맞아도_통과한다() {
        allowDomains("portal.example.com", "*.partner.com");

        assertDoesNotThrow(() -> validator.validate(referer("https://shop.partner.com/x"), workflow));
    }

    @Test
    void Referer가_없으면_Origin을_본다() {
        allowDomains("portal.example.com");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DomainAccessValidator.ORIGIN_HEADER, "https://portal.example.com");

        assertDoesNotThrow(() -> validator.validate(request, workflow));
    }

    @Test
    void Referer와_Origin이_모두_없으면_막는다() {
        allowDomains("portal.example.com");

        assertThrows(BusinessException.class, () -> validator.validate(referer(null), workflow));
    }

    @Test
    void 요청이_없는_실행은_도메인제한을_적용하지않는다() {
        allowDomains("portal.example.com");

        assertDoesNotThrow(() -> validator.validate(null, workflow));
    }
}

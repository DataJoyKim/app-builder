package com.prometis.appbuilder.security.domainaccess;

import com.prometis.appbuilder.workflow.Workflow;
import com.prometis.appbuilder.workflow.WorkflowDomain;
import com.prometis.appbuilder.workflow.WorkflowDomainRepository;
import com.prometis.appbuilder.workflow.WorkflowErrorMessage;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 도메인 접근제어. 워크플로우에 매핑된 도메인에서 호출한 요청만 실행할 수 있다.
 *
 * - 매핑된 도메인이 없으면 도메인 제한을 쓰지 않는 워크플로우로 보고 통과시킨다.
 * - 접근 도메인은 Referer 헤더에서 읽고, 없으면 Origin 헤더를 본다.
 *   둘 다 없으면(서버 간 호출, 직접 호출 등) 확인할 방법이 없으므로 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainAccessValidator {
    public static final String REFERER_HEADER = "Referer";
    public static final String ORIGIN_HEADER = "Origin";

    private final WorkflowDomainRepository workflowDomainRepository;

    public void validate(HttpServletRequest request, Workflow workflow) throws BusinessException {
        List<WorkflowDomain> workflowDomains = workflowDomainRepository.findByWorkflow(workflow);

        if(workflowDomains.isEmpty()) {
            return;
        }

        // 스케줄러처럼 서버 안에서 실행할 때는 요청 자체가 없으므로 도메인 제한을 적용하지않는다.
        if(request == null) {
            return;
        }

        String domain = resolveDomain(request);

        if(domain == null) {
            log.warn("접근 도메인(Referer)을 확인할 수 없어 실행을 막았습니다. [workflowCode:{}]", workflow.getWorkflowCode());
            throw new BusinessException(WorkflowErrorMessage.NOT_ALLOWED_DOMAIN);
        }

        for(WorkflowDomain workflowDomain : workflowDomains) {
            if(DomainMatcher.matches(workflowDomain.getDomain(), domain)) {
                return;
            }
        }

        log.warn("허용되지않은 도메인의 실행 요청입니다. [workflowCode:{}, domain:{}]", workflow.getWorkflowCode(), domain);
        throw new BusinessException(WorkflowErrorMessage.NOT_ALLOWED_DOMAIN);
    }

    private static String resolveDomain(HttpServletRequest request) {
        String referer = DomainMatcher.hostOf(request.getHeader(REFERER_HEADER));

        return (referer != null) ? referer : DomainMatcher.hostOf(request.getHeader(ORIGIN_HEADER));
    }
}

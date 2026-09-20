package com.prometis.appbuilder.security.ip;

import com.prometis.appbuilder.workflow.Workflow;
import com.prometis.appbuilder.workflow.WorkflowErrorMessage;
import com.prometis.appbuilder.workflow.WorkflowIpGroup;
import com.prometis.appbuilder.workflow.WorkflowIpGroupRepository;
import com.prometis.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * IP 접근제어. 워크플로우에 매핑된 IP 그룹의 IP 목록에 접속 IP 가 들어있어야 실행할 수 있다.
 *
 * - 매핑된 IP 그룹이 없으면 IP 제한을 쓰지 않는 워크플로우로 보고 통과시킨다.
 * - 매핑은 있는데 허용 IP 가 하나도 없거나 접속 IP 를 알 수 없으면 막는다. (제한을 걸어둔 워크플로우이므로)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpAccessValidator {
    private final WorkflowIpGroupRepository workflowIpGroupRepository;
    private final IpGroupRepository ipGroupRepository;
    private final IpAddressRepository ipAddressRepository;

    public void validate(HttpServletRequest request, Workflow workflow) throws BusinessException {
        List<WorkflowIpGroup> workflowIpGroups = workflowIpGroupRepository.findByWorkflow(workflow);

        if(workflowIpGroups.isEmpty()) {
            return;
        }

        // 스케줄러처럼 서버 안에서 실행할 때는 접속 IP 자체가 없으므로 IP 제한을 적용하지않는다.
        if(request == null) {
            return;
        }

        String clientIp = ClientIpResolver.resolve(request);

        if(clientIp == null) {
            log.warn("접속 IP 를 확인할 수 없어 실행을 막았습니다. [workflowCode:{}]", workflow.getWorkflowCode());
            throw new BusinessException(WorkflowErrorMessage.NOT_ALLOWED_IP);
        }

        if(isAllowed(workflowIpGroups, clientIp)) {
            return;
        }

        log.warn("허용되지않은 IP 의 실행 요청입니다. [workflowCode:{}, ip:{}]", workflow.getWorkflowCode(), clientIp);
        throw new BusinessException(WorkflowErrorMessage.NOT_ALLOWED_IP);
    }

    private boolean isAllowed(List<WorkflowIpGroup> workflowIpGroups, String clientIp) {
        List<String> groupCodes = workflowIpGroups.stream()
                .map(WorkflowIpGroup::getIpGroupCode)
                .toList();

        List<Long> groupIds = ipGroupRepository.findByGroupCodeIn(groupCodes).stream()
                .map(IpGroup::getId)
                .toList();

        if(groupIds.isEmpty()) {
            return false;
        }

        for(IpAddress ipAddress : ipAddressRepository.findByIpGroupIdIn(groupIds)) {
            if(!ipAddress.isEnabled()) {
                continue;
            }

            if(IpAddressMatcher.matches(ipAddress.getIpAddress(), clientIp)) {
                return true;
            }
        }

        return false;
    }
}

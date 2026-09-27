package com.prometis.appbuilder.console.app;

import com.prometis.appbuilder.app.workflow.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 워크플로우 하위 행(노드/연결정보/조건/에러응답)은 workflowId 만 갖고 있어서,
 * 콘솔이 그 행을 다룰 때 워크플로우가 요청한 애플리케이션 것인지 여기서 확인한다.
 */
@Component
@RequiredArgsConstructor
public class AppConsoleWorkflowScope {
    private final WorkflowRepository workflowRepository;

    public boolean owns(String applicationId, Long workflowId) {
        if(applicationId == null || workflowId == null) {
            return false;
        }

        return workflowRepository.findById(workflowId)
                .filter(workflow -> applicationId.equals(workflow.getApplicationId()))
                .isPresent();
    }

    public void requireOwned(String applicationId, Long workflowId) {
        if(!owns(applicationId, workflowId)) {
            throw new RuntimeException("요청한 워크플로우가 존재하지않습니다. [workflowId:" + workflowId + "]");
        }
    }
}

package com.prometis.appbuilder.app.node;

import com.prometis.appbuilder.app.node.code.FunctionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, Long> {
    List<WorkflowNode> findByWorkflowIdOrderByOrderNum(Long workflowId);

    void deleteByWorkflowId(Long workflowId);

    List<WorkflowNode> findByWorkflowId(Long workflowId);

    /**
     * 애플리케이션의 워크플로우 실행(WORKFLOW) 노드가 만드는 호출관계 [호출하는 워크플로우코드, 실행할 워크플로우코드].
     */
    @Query("select distinct w.workflowCode, n.functionName from WorkflowNode n, Workflow w " +
            "where n.workflowId = w.id and w.applicationId = :applicationId and n.functionType = :functionType")
    List<Object[]> findWorkflowCalls(@Param("applicationId") String applicationId,
                                     @Param("functionType") FunctionType functionType);
}

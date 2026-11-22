package com.prometis.appbuilder.app.workflow.log;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, Long>, JpaSpecificationExecutor<WorkflowExecutionLog> {
    // 보관 기간 정리(LogCleanupService). 한 문장으로 지우므로 엔티티를 읽어 오지 않는다
    @Modifying
    @Query("delete from WorkflowExecutionLog l where l.executedDate < :before")
    int deleteByDateBefore(@Param("before") LocalDate before);

    // [executedDate, resultType, count]
    @Query("select l.executedDate, l.resultType, count(l) from WorkflowExecutionLog l " +
            "where l.applicationId = :applicationId and l.executedDate >= :from " +
            "group by l.executedDate, l.resultType")
    List<Object[]> countDaily(@Param("applicationId") String applicationId, @Param("from") LocalDate from);

    // [평균 처리시간(ms), 실행한 사용자 수] 한 줄
    @Query("select avg(l.durationMs), count(distinct l.userId) from WorkflowExecutionLog l " +
            "where l.applicationId = :applicationId and l.executedDate >= :from")
    List<Object[]> summary(@Param("applicationId") String applicationId, @Param("from") LocalDate from);

    // [workflowCode, 실행 수, 실패 수] 실행 수 많은 순
    @Query("select l.workflowCode, count(l), sum(case when l.resultType = 'SUCCESS' then 0 else 1 end) " +
            "from WorkflowExecutionLog l " +
            "where l.applicationId = :applicationId and l.executedDate >= :from and l.workflowCode is not null " +
            "group by l.workflowCode order by count(l) desc")
    List<Object[]> countByWorkflow(@Param("applicationId") String applicationId, @Param("from") LocalDate from, Pageable pageable);
}

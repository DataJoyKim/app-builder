package com.prometis.appbuilder.app.workflow.log;

import com.prometis.appbuilder.app.workflow.code.WorkflowTrigger;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 워크플로우 실행 로그. 바깥 워크플로우 실행 한 번에 한 줄이다(워크플로우 실행 노드로 부른 하위 워크플로우는 따로 남기지 않는다).
 * 결과는 응답의 상태/코드/메시지까지만 남긴다. 응답 본문(contents)은 개인정보가 들어 있을 수 있고 크기도 정해져 있지 않아 남기지 않는다.
 * executedDate 는 일별 집계(콘솔 대시보드)를 DB 종류와 상관없이 하려고 따로 둔다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "WF_EXEC_LOG_IX1", columnList = "applicationId,executedDate"),
        @Index(name = "WF_EXEC_LOG_IX2", columnList = "applicationId,workflowCode")
})
@Entity
public class WorkflowExecutionLog {
    public static final int MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String applicationId;
    @Column(length = 100)
    private String workflowCode;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WorkflowTrigger triggerType;
    // 워크플로우를 부른 화면(RequestMessage.Header.objectCode). 화면 밖에서 부르면 비어 있다.
    @Column(length = 100)
    private String objectCode;
    // 익명 실행이면 비어 있다
    @Column
    private Long userId;
    @Column(length = 200)
    private String userName;
    @Column(length = 64)
    private String clientIp;
    // ResponseMessage.resultType (SUCCESS / ERROR)
    @Column(nullable = false, length = 20)
    private String resultType;
    @Column
    private Integer status;
    @Column(length = 50)
    private String code;
    @Column(length = MESSAGE_MAX_LENGTH)
    private String message;
    @Column
    private Long durationMs;
    @Column(nullable = false)
    private LocalDateTime executedAt;
    @Column(nullable = false)
    private LocalDate executedDate;
}

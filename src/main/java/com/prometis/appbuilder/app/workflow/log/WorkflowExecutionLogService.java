package com.prometis.appbuilder.app.workflow.log;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.dto.ResponseMessage;
import com.prometis.appbuilder.app.log.AccessLogWriter;
import com.prometis.appbuilder.app.log.AccessUserResolver;
import com.prometis.appbuilder.app.util.ClientIpResolver;
import com.prometis.appbuilder.app.workflow.code.WorkflowTrigger;
import com.prometis.appbuilder.security.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 애플리케이션별 워크플로우 실행 로그. 누가(userId) 언제(executedAt) 어떤 경로로(triggerType) 실행했고 결과가 어땠는지 남긴다.
 */
@Service
@RequiredArgsConstructor
public class WorkflowExecutionLogService {
    private final WorkflowExecutionLogRepository workflowExecutionLogRepository;
    private final AccessUserResolver accessUserResolver;
    private final AccessLogWriter accessLogWriter;

    public void record(
            HttpServletRequest request,
            String applicationId,
            RequestMessage requestMessage,
            AuthenticatedUser user,
            ResponseMessage response,
            LocalDateTime executedAt,
            long durationMs
    ) {
        accessLogWriter.write("워크플로우 실행", () -> {
            RequestMessage.Header header = requestMessage == null ? null : requestMessage.getHeader();
            AuthenticatedUser actor = accessUserResolver.resolve(request, user);
            WorkflowTrigger trigger = header == null || header.getTrigger() == null ? WorkflowTrigger.WEB : header.getTrigger();

            workflowExecutionLogRepository.save(WorkflowExecutionLog.builder()
                    .applicationId(applicationId)
                    .workflowCode(header == null ? null : AccessLogWriter.truncate(header.getWorkflowCode(), 100))
                    .triggerType(trigger)
                    .objectCode(header == null ? null : AccessLogWriter.truncate(header.getObjectCode(), 100))
                    .userId(actor == null ? null : actor.getUserId())
                    .userName(actor == null ? null : AccessLogWriter.truncate(actor.getUserName(), 200))
                    .clientIp(AccessLogWriter.truncate(ClientIpResolver.resolve(request), 64))
                    .resultType(response.getResultType() == null ? "ERROR" : response.getResultType().name())
                    .status(response.getStatus())
                    .code(AccessLogWriter.truncate(response.getCode(), 50))
                    .message(AccessLogWriter.truncate(response.getMessage(), WorkflowExecutionLog.MESSAGE_MAX_LENGTH))
                    .durationMs(durationMs)
                    .executedAt(executedAt)
                    .executedDate(executedAt.toLocalDate())
                    .build());
        });
    }
}

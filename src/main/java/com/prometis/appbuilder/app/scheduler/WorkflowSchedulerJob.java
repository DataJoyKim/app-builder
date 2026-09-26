package com.prometis.appbuilder.app.scheduler;

import com.prometis.appbuilder.app.dto.RequestMessage;
import com.prometis.appbuilder.app.dto.ResponseMessage;
import com.prometis.appbuilder.app.dto.ResultType;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJobHistory;
import com.prometis.appbuilder.app.scheduler.domain.SchedulerJobWorkflow;
import com.prometis.appbuilder.app.workflow.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Quartz가 매 실행마다 리플렉션으로 인스턴스를 생성하므로 @Component로 등록하지 않는다.
// Spring Boot의 quartz 자동구성이 등록하는 JobFactory가 아래 필드들을 자동 주입해준다.
@Slf4j
public class WorkflowSchedulerJob implements Job {
    @Autowired
    private SchedulerJobRepository schedulerJobRepository;
    @Autowired
    private SchedulerJobWorkflowRepository schedulerJobWorkflowRepository;
    @Autowired
    private SchedulerJobHistoryRepository schedulerJobHistoryRepository;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void execute(JobExecutionContext context) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();

        Long schedulerJobId = context.getJobDetail().getJobDataMap().getLong("schedulerJobId");

        SchedulerJob schedulerJob = schedulerJobRepository.findById(schedulerJobId).orElse(null);
        if(schedulerJob == null) {
            log.warn("SchedulerJob(id={})를 찾을 수 없어 실행을 건너뜁니다.", schedulerJobId);
            return;
        }

        SchedulerJobHistory history = schedulerJobHistoryRepository.save(
                SchedulerJobHistory.builder()
                        .schedulerJob(schedulerJob)
                        .startedAt(LocalDateTime.now())
                        .build()
        );

        List<SchedulerJobWorkflow> jobWorkflows = schedulerJobWorkflowRepository.findBySchedulerJobIdOrderByOrderNum(schedulerJobId);

        int successCount = 0;
        StringBuilder message = new StringBuilder();

        for(SchedulerJobWorkflow jobWorkflow : jobWorkflows) {
            String workflowCode = jobWorkflow.getWorkflow().getWorkflowCode();

            try {
                RequestMessage requestMessage = buildRequestMessage(workflowCode, jobWorkflow.getRequestMessageJson());

                ResponseMessage responseMessage = workflowService.execute(request, response, requestMessage);

                if(ResultType.SUCCESS.equals(responseMessage.getResultType())) {
                    successCount++;
                }
                else {
                    message.append(workflowCode).append(": ").append(responseMessage.getMessage()).append("\n");
                }
            }
            catch (Exception e) {
                log.error("워크플로우({}) 실행 중 오류가 발생했습니다.", workflowCode, e);
                message.append(workflowCode).append(": ").append(e.getMessage()).append("\n");
            }
        }

        boolean success = (successCount == jobWorkflows.size());
        String summary = success
                ? String.format("%d/%d 워크플로우 실행 성공", successCount, jobWorkflows.size())
                : String.format("%d/%d 워크플로우 실행 성공\n%s", successCount, jobWorkflows.size(), message);

        history.finish(success, summary);
        schedulerJobHistoryRepository.save(history);
    }

    @SuppressWarnings("unchecked")
    private RequestMessage buildRequestMessage(String workflowCode, String requestMessageJson) throws Exception {
        RequestMessage requestMessage = new RequestMessage();

        RequestMessage.Header header = new RequestMessage.Header();
        header.setWorkflowCode(workflowCode);
        requestMessage.setHeader(header);

        Map<String, List<Map<String, Object>>> body;
        if(requestMessageJson == null || requestMessageJson.isBlank()) {
            body = new HashMap<>();
        }
        else {
            body = objectMapper.readValue(requestMessageJson, Map.class);
        }
        requestMessage.setBody(body);

        return requestMessage;
    }
}

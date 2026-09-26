package com.prometis.appbuilder.app.scheduler;

import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 앱 기동 시 활성화된 스케줄러 Job들을 Quartz에 등록한다.
// (memory job-store는 재기동될 때마다 비워지므로, DB에 저장된 설정을 매번 다시 등록해야 한다)
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerBootstrap implements ApplicationRunner {
    private final SchedulerJobRepository schedulerJobRepository;
    private final SchedulerManagementService schedulerManagementService;

    @Override
    public void run(ApplicationArguments args) {
        List<SchedulerJob> enabledJobs = schedulerJobRepository.findByEnabledTrue();

        for(SchedulerJob job : enabledJobs) {
            try {
                schedulerManagementService.scheduleOrReplace(job);
            }
            catch (Exception e) {
                log.error("스케줄러 Job({}) 등록 중 오류가 발생했습니다.", job.getJobCode(), e);
            }
        }
    }
}

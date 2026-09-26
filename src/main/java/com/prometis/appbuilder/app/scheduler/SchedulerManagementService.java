package com.prometis.appbuilder.app.scheduler;

import com.prometis.appbuilder.app.scheduler.domain.SchedulerJob;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerManagementService {
    private static final String JOB_GROUP = "SCHEDULER_JOB";

    private final Scheduler scheduler;

    public boolean isStarted() throws SchedulerException {
        return scheduler.isStarted() && !scheduler.isInStandbyMode() && !scheduler.isShutdown();
    }

    public void start() throws SchedulerException {
        if(scheduler.isShutdown()) {
            return;
        }

        scheduler.start();
    }

    // 웹콘솔의 "종료"는 재시작 가능한 일시정지(standby)로 처리한다. 완전 shutdown()은 앱 재기동 전까지 복구가 불가능하다.
    public void standby() throws SchedulerException {
        scheduler.standby();
    }

    public void scheduleOrReplace(SchedulerJob job) throws SchedulerException {
        JobKey jobKey = jobKey(job.getJobCode());

        if(scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }

        if(!Boolean.TRUE.equals(job.getEnabled())) {
            return;
        }

        JobDetail jobDetail = JobBuilder.newJob(WorkflowSchedulerJob.class)
                .withIdentity(jobKey)
                .usingJobData("schedulerJobId", job.getId())
                .storeDurably()
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(job.getJobCode()))
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void pauseJob(String jobCode) throws SchedulerException {
        scheduler.pauseJob(jobKey(jobCode));
    }

    public void resumeJob(SchedulerJob job) throws SchedulerException {
        if(scheduler.checkExists(jobKey(job.getJobCode()))) {
            scheduler.resumeJob(jobKey(job.getJobCode()));
        }
        else {
            scheduleOrReplace(job);
        }
    }

    public void restartJob(SchedulerJob job) throws SchedulerException {
        scheduleOrReplace(job);
    }

    public void triggerNow(String jobCode) throws SchedulerException {
        scheduler.triggerJob(jobKey(jobCode));
    }

    public void unscheduleJob(String jobCode) throws SchedulerException {
        JobKey jobKey = jobKey(jobCode);
        if(scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    public List<Date> previewNextFireTimes(String cronExpression, int count) throws ParseException {
        CronExpression expression = new CronExpression(cronExpression);

        List<Date> results = new ArrayList<>();
        Date current = new Date();
        for(int i = 0; i < count; i++) {
            current = expression.getNextValidTimeAfter(current);
            if(current == null) {
                break;
            }
            results.add(current);
        }

        return results;
    }

    // 등록된 트리거가 없거나(비활성/미등록) 조회에 실패하면 null을 반환한다.
    public Date getNextFireTime(String jobCode) {
        try {
            Trigger trigger = scheduler.getTrigger(triggerKey(jobCode));
            return (trigger != null) ? trigger.getNextFireTime() : null;
        }
        catch (SchedulerException e) {
            return null;
        }
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private JobKey jobKey(String jobCode) {
        return JobKey.jobKey(jobCode, JOB_GROUP);
    }

    private TriggerKey triggerKey(String jobCode) {
        return TriggerKey.triggerKey(jobCode, JOB_GROUP);
    }
}

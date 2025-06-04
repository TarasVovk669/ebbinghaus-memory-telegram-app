package com.ebbinghaus.memory.app.utils;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.model.ScheduleResultTuple;
import com.ebbinghaus.memory.app.service.impl.SchedulerServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

import static com.ebbinghaus.memory.app.utils.Constants.*;
import static java.time.ZoneOffset.UTC;

public class SchedulerUtils {

    @NotNull
    public static ScheduleResultTuple getScheduleResultTuple(
            EMessage message, Long chatId, JobDataMap jobDataMap) {
        var jobDetail =
                JobBuilder.newJob(SchedulerServiceImpl.class)
                        .withIdentity(message.getId().toString().concat(chatId.toString()), JOBS_GROUP)
                        .withDescription(String.format("Send scheduled message: %d", message.getId()))
                        .usingJobData(jobDataMap)
                        .storeDurably()
                        .build();
        var trigger =
                TriggerBuilder.newTrigger()
                        .forJob(jobDetail)
                        .withIdentity(jobDetail.getKey().getName(), TRIGGERS_GROUP)
                        .withDescription(
                                String.format("Send Message Trigger: %s", jobDetail.getKey().getName()))
                        .startAt(Date.from(message.getNextExecutionDateTime().atZone(UTC).toInstant()))
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                        .build();
        return new ScheduleResultTuple(jobDetail, trigger);
    }

    @NotNull
    public static ScheduleResultTuple getScheduleResultTuple(Long userId, DayOfWeek day, LocalTime time, JobDataMap jobDataMap) {
        var jobDetail =
                JobBuilder.newJob(SchedulerServiceImpl.class)
                        .withIdentity(userId.toString().concat(day.toString()), JOBS_GROUP)
                        .withDescription(String.format("Send remainder quiz for user: %d", userId))
                        .usingJobData(jobDataMap)
                        .storeDurably()
                        .build();
        var trigger =
                TriggerBuilder.newTrigger()
                        .forJob(jobDetail)
                        .withIdentity(jobDetail.getKey().getName(), TRIGGERS_GROUP)
                        .withDescription(
                                String.format("Send Message Trigger: %s", jobDetail.getKey().getName()))
                        .startAt(Date.from(LocalDate.now()
                                .with(TemporalAdjusters.nextOrSame(day))
                                .atTime(time).atZone(UTC).toInstant()))
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                        .build();
        return new ScheduleResultTuple(jobDetail, trigger);
    }

    public static void initRetryCounts(JobDataMap jobDataMap) {
        jobDataMap.putAsString(FIB_STEP_FIRST, 0);
        jobDataMap.putAsString(FIB_STEP_SECOND, 1);
    }
}

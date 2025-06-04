package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.domain.EUser;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.ScheduleResultTuple;
import com.ebbinghaus.memory.app.service.SchedulerService;
import com.ebbinghaus.memory.app.service.UserService;
import com.ebbinghaus.memory.app.service.impl.strategy.scheduler.SchedulerStrategy;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static com.ebbinghaus.memory.app.utils.Constants.DEFAULT;
import static com.ebbinghaus.memory.app.utils.Constants.QUIZ_REMAINDER;
import static com.ebbinghaus.memory.app.utils.SchedulerUtils.getScheduleResultTuple;
import static com.ebbinghaus.memory.app.utils.SchedulerUtils.initRetryCounts;
import static java.time.ZoneOffset.UTC;

@Component
@RequiredArgsConstructor
public class SchedulerServiceImpl extends QuartzJobBean implements SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private final Scheduler scheduler;
    private final UserService userService;
    private final Map<String, SchedulerStrategy> schedulerMap;

    @Override
    @Transactional
    protected void executeInternal(JobExecutionContext context) {
        log.info("Executing job with key: {}", context.getJobDetail().getKey());

        var jobDataMap = context.getJobDetail().getJobDataMap();
        Optional.ofNullable(jobDataMap.getString("type"))
                .map(schedulerMap::get)
                .orElse(schedulerMap.get(DEFAULT))
                .process(context, jobDataMap);
    }

    @Override
    public void scheduleMessage(EMessage message, InputUserData userData) {
        log.info("Schedule message with id: {} and chat_id:{}", message.getId(), userData.getChatId());

        var jobDataMap = new JobDataMap();
        jobDataMap.putAsString("message_id", message.getId());
        jobDataMap.putAsString("chat_id", userData.getChatId());
        initRetryCounts(jobDataMap);

        try {
            ScheduleResultTuple result =
                    getScheduleResultTuple(message, userData.getChatId(), jobDataMap);
            scheduler.scheduleJob(result.jobDetail(), result.trigger());

            log.info(
                    "Create trigger for message with id: {} and for chat_id: {}",
                    message.getId(),
                    userData.getChatId());
        } catch (SchedulerException e) {
            log.error("Error scheduling message", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void scheduleQuizRemainderMessage() {
        userService.findAllActiveUsers()
                .forEach(user -> {
                    var time = LocalTime.of(14, 0);

                    scheduleMessage(user, DayOfWeek.TUESDAY, LocalTime.now(UTC).plusSeconds(15));
                    //scheduleMessage(user, DayOfWeek.TUESDAY, time);
                    //scheduleMessage(user, DayOfWeek.FRIDAY, time);
                });
    }

    private void scheduleMessage(EUser user, DayOfWeek day, LocalTime time) {
        log.info("Schedule quiz remainder message for user: {} on {}", user.getId(), day);

        var jobDataMap = new JobDataMap();
        jobDataMap.put("type", QUIZ_REMAINDER);
        jobDataMap.putAsString("chat_id", user.getId());
        initRetryCounts(jobDataMap);

        try {
            ScheduleResultTuple result =
                    getScheduleResultTuple(user.getId(), day, time, jobDataMap);
            scheduler.scheduleJob(result.jobDetail(), result.trigger());

            log.info(
                    "Create trigger for chat_id: {} with type: {}",
                    user.getId(),
                    QUIZ_REMAINDER);
        } catch (SchedulerException e) {
            log.error("Error scheduling message", e);
        }
    }
}

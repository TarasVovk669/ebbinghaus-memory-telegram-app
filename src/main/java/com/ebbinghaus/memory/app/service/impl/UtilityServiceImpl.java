package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.service.UtilityService;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.ebbinghaus.memory.app.utils.Constants.JOBS_GROUP;
import static com.ebbinghaus.memory.app.utils.Constants.TRIGGERS_GROUP;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static java.time.ZoneOffset.UTC;

@Component
@RequiredArgsConstructor
public class UtilityServiceImpl implements UtilityService {

  private static final Logger log = LoggerFactory.getLogger(SchedulerServiceImpl.class);

  private final Scheduler scheduler;

  @Override
  public void removeSchedulerTrigger(Long id, Long chatId) {
    log.info("Remove message trigger from table for message_id: {} and chat_id: {}", id, chatId);
    String key = id.toString().concat(chatId.toString());
    JobKey jobKey = JobKey.jobKey(key, JOBS_GROUP);

    boolean jobDeleted = doTry(() -> scheduler.deleteJob(jobKey));
    log.info(
        jobDeleted ? "Successfully deleted job with key: {}" : "Failed to delete job with key: {}",
        key);
  }

  @Override
  public void rescheduleJob(EMessage message, Long chatId) {
    log.info("Reschedule job with message: {} and chat_id: {}", message, chatId);

    var key = message.getId().toString().concat(chatId.toString());
    var jobDetail = doTry(() -> scheduler.getJobDetail(JobKey.jobKey(key, JOBS_GROUP)));
    var triggerKey = TriggerKey.triggerKey(key, TRIGGERS_GROUP);

    var newTrigger =
        TriggerBuilder.newTrigger()
            .forJob(jobDetail)
            .withIdentity(triggerKey)
            .withDescription(
                String.format("Send Message Trigger: %s", jobDetail.getKey().getName()))
            .startAt(Date.from(message.getNextExecutionDateTime().atZone(UTC).toInstant()))
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
            .build();

    if (doTry(() -> scheduler.checkExists(triggerKey))) {
      log.info("Reschedule job with key: {}", triggerKey);
      doTry(() -> scheduler.rescheduleJob(triggerKey, newTrigger));
    } else {
      log.info("Create trigger for job with key: {}", triggerKey);
      doTry(() -> scheduler.scheduleJob(newTrigger));
    }
  }
}

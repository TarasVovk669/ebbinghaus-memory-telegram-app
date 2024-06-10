package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.service.UtilityService;
import lombok.RequiredArgsConstructor;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.ebbinghaus.memory.app.service.impl.SchedulerServiceImpl.JOBS_GROUP;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;


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
        log.info(jobDeleted
                        ? "Successfully deleted job with key: {}"
                        : "Failed to delete job with key: {}",
                key);
    }
}

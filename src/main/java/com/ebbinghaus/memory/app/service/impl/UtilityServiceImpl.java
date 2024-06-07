package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.service.UtilityService;
import lombok.RequiredArgsConstructor;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;


@Component
@RequiredArgsConstructor
public class UtilityServiceImpl implements UtilityService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private final Scheduler scheduler;

    @Override
    public void removeSchedulerTrigger(Long id, Long chatId) {
        log.info("Remove message trigger from table for message_id: {} and chat_id: {}", id, chatId);

        doTry(() -> scheduler.deleteJob(JobKey.jobKey(id.toString().concat(chatId.toString()))));
    }
}

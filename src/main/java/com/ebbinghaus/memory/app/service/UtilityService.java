package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.EMessage;

public interface UtilityService {

    void removeSchedulerTrigger(Long id, Long chatId);

    void removeSchedulerTrigger(String key);

    void rescheduleJob(EMessage message, Long chatId);
}

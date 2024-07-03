package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.EMessage;

public interface UtilityService {

  void removeSchedulerTrigger(Long id, Long chatId);

  void rescheduleJob(EMessage message, Long chatId);
}

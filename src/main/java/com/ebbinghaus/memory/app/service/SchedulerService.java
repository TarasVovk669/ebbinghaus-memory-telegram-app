package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.model.InputUserData;

public interface SchedulerService {
    void scheduleMessage(EMessage message, InputUserData userData);
}

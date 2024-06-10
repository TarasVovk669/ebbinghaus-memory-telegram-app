package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.ScheduleMessageErrorQueue;
import com.ebbinghaus.memory.app.repository.ScheduleMessageErrorQueueRepository;
import com.ebbinghaus.memory.app.service.ScheduleMessageErrorQueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleMessageErrorQueueServiceImpl implements ScheduleMessageErrorQueueService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleMessageErrorQueueServiceImpl.class);

    private final ScheduleMessageErrorQueueRepository scheduleMessageErrorQueueRepository;

    @Override
    public ScheduleMessageErrorQueue save(ScheduleMessageErrorQueue errorMessage) {
        log.info("Save error_schedule_message: {}", errorMessage);
        return scheduleMessageErrorQueueRepository.save(errorMessage);
    }
}

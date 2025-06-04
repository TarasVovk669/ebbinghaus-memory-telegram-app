package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeeklyQuizSchedulerServiceImpl  {

    private final SchedulerService schedulerService;

    //@Scheduled(cron = "0 0 12 * * SUN")
    @Async
    @Scheduled(cron = "0 * * * * *")
    public void scheduleQuizJobs() {
        schedulerService.scheduleQuizRemainderMessage();
    }
}

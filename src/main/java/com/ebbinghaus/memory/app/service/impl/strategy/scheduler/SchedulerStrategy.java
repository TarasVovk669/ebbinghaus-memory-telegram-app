package com.ebbinghaus.memory.app.service.impl.strategy.scheduler;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

public interface SchedulerStrategy {
    void process(JobExecutionContext context, JobDataMap jobDataMap);
}

package com.ebbinghaus.memory.app.model;

import org.quartz.JobDetail;
import org.quartz.Trigger;

public record ScheduleResultTuple(JobDetail jobDetail, Trigger trigger) {

}

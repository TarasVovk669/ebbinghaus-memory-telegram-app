package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.domain.ScheduleMessageErrorQueue;
import com.ebbinghaus.memory.app.model.CategoryMessageCount;
import com.ebbinghaus.memory.app.model.proj.CategoryMessageCountProj;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ScheduleMessageErrorQueueService {

    ScheduleMessageErrorQueue save(ScheduleMessageErrorQueue errorQueue);

}

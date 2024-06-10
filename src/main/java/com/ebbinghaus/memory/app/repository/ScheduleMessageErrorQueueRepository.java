package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.ScheduleMessageErrorQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleMessageErrorQueueRepository extends JpaRepository<ScheduleMessageErrorQueue, Long> {

}

package com.ebbinghaus.memory.app.config;

import com.ebbinghaus.memory.app.service.impl.strategy.scheduler.MessageSchedulerStrategy;
import com.ebbinghaus.memory.app.service.impl.strategy.scheduler.QuizRemainderStrategy;
import com.ebbinghaus.memory.app.service.impl.strategy.scheduler.SchedulerStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.ebbinghaus.memory.app.utils.Constants.DEFAULT;
import static com.ebbinghaus.memory.app.utils.Constants.QUIZ_REMAINDER;

@Configuration
public class SchedulerMapConfig {

    @Bean
    public Map<String, SchedulerStrategy> schedulerMap(
            QuizRemainderStrategy quizRemainderStrategy, MessageSchedulerStrategy messageSchedulerStrategy) {
        return Map.of(QUIZ_REMAINDER, quizRemainderStrategy, DEFAULT, messageSchedulerStrategy);
    }
}

package com.ebbinghaus.memory.app.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "virtualTaskExecutor")
    public Executor virtualTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("virtual-task-");
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(mdcTaskDecorator());
        return executor;
    }

    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
}

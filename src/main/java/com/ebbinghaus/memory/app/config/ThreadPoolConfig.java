package com.ebbinghaus.memory.app.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPoolConfig {

    private final Integer threadCount;

    public ThreadPoolConfig(@Value("${app.thread-count:2}") Integer threadCount) {
        this.threadCount = threadCount;
    }

    @Bean(name = "ioTaskExecutor")
    public Executor ioTaskExecutor() {
        return Executors.newFixedThreadPool(threadCount);
    }

    @Bean(name = "virtualTaskExecutor")
    public Executor virtualTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

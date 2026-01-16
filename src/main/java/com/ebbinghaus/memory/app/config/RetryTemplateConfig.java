package com.ebbinghaus.memory.app.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class RetryTemplateConfig {
    @Bean("quizJsonProcessorRetryTemplate")
    public RetryTemplate quizJsonProcessorRetryTemplate() {

        var backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000L);

        var policy = new SimpleRetryPolicy(
                3,
                Map.of(
                        JsonProcessingException.class, true,
                        JsonMappingException.class, true,
                        Exception.class, false));
        return RetryTemplate.builder()
                .customPolicy(policy)
                .customBackoff(backOffPolicy)
                .build();
    }
}

package com.ebbinghaus.memory.app.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Component;

import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;

@Component
@RequiredArgsConstructor
public class QuartzMetricsBinder implements MeterBinder {

    private final Scheduler scheduler;

    @Override
    public void bindTo(@NotNull MeterRegistry registry) {
        Gauge.builder(
                        "quartz.jobs.total",
                        scheduler,
                        s -> doTry(() -> s.getJobKeys(GroupMatcher.anyGroup()).size()))
                .description("Total number of Quartz jobs")
                .register(registry);

        Gauge.builder(
                        "quartz.jobs.executing",
                        scheduler,
                        s -> doTry(() -> s.getCurrentlyExecutingJobs().size()))
                .description("Currently executing Quartz jobs")
                .register(registry);
    }
}

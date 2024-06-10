package com.ebbinghaus.memory.app.utils;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.service.MessageSourceService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;

import static com.ebbinghaus.memory.app.utils.Constants.DEFAULT_TIME_EXECUTION;
import static com.ebbinghaus.memory.app.utils.Constants.INTERVAL_MAP;

public class DateUtils {

    //for new messages
    public static LocalDateTime calculateNextExecutionTime(LocalDateTime dateTime) {
        return dateTime.plusMinutes(DEFAULT_TIME_EXECUTION);
    }

    public static LocalDateTime calculateNextExecutionTime(EMessage eMessage) {
        return eMessage.getNextExecutionDateTime()
                .plusMinutes(
                        INTERVAL_MAP.getOrDefault(
                                eMessage.getExecutionStep(),
                                DEFAULT_TIME_EXECUTION));
    }

    public static String formatDuration(LocalDateTime start,
                                        LocalDateTime end,
                                        String languageCode,
                                        MessageSourceService messageSourceService) {
        if (end.isBefore(start)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }

        Period period = Period.between(start.toLocalDate(), end.toLocalDate());
        Duration duration = Duration.between(start.toLocalTime(), end.toLocalTime());

        if (duration.isNegative()) {
            period = period.minusDays(1);
            duration = duration.plusDays(1);
        }

        StringBuilder result = new StringBuilder();
        if (period.getYears() != 0)
            result.append(period.getYears()).append(messageSourceService.getMessage("messages.execution-time.years", languageCode));
        if (period.getMonths() != 0)
            result.append(period.getMonths()).append(messageSourceService.getMessage("messages.execution-time.months", languageCode));
        if (period.getDays() != 0)
            result.append(period.getDays()).append(messageSourceService.getMessage("messages.execution-time.days", languageCode));
        if (duration.toHours() != 0)
            result.append(duration.toHours() % 24).append(messageSourceService.getMessage("messages.execution-time.hours", languageCode));
        if (duration.toMinutes() % 60 != 0)
            result.append(duration.toMinutes() % 60).append(messageSourceService.getMessage("messages.execution-time.minutes", languageCode));
        if (result.isEmpty())
            result.append(messageSourceService.getMessage("messages.execution-time.minute", languageCode));

        return result.toString().trim();
    }
}

package com.ebbinghaus.memory.app.utils;

import com.ebbinghaus.memory.app.domain.EMessage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;

import static com.ebbinghaus.memory.app.utils.Constants.DEFAULT_TIME_EXECUTION;
import static com.ebbinghaus.memory.app.utils.Constants.INTERVAL_MAP;

public class DateUtils {

    //for new messages
    public static LocalDateTime calculateNextExecutionTime(LocalDateTime dateTime) {
        return dateTime.plusHours(DEFAULT_TIME_EXECUTION);
    }

    public static LocalDateTime calculateNextExecutionTime(EMessage eMessage) {
        return eMessage.getNextExecutionDateTime()
                .plusHours(
                        INTERVAL_MAP.getOrDefault(
                                eMessage.getExecutionStep(),
                                DEFAULT_TIME_EXECUTION));
    }

    //todo: format this for future with correct endings
    public static String formatDuration(LocalDateTime start, LocalDateTime end) {
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
        if (period.getYears() != 0) result.append(period.getYears()).append(" years ");
        if (period.getMonths() != 0) result.append(period.getMonths()).append(" months ");
        if (period.getDays() != 0) result.append(period.getDays()).append(" days ");
        if (duration.toHours() != 0) result.append(duration.toHours() % 24).append(" hours ");
        if (duration.toMinutes() % 60 != 0) result.append(duration.toMinutes() % 60).append(" minutes ");
        if (result.isEmpty()) result.append(" < 1 minute");

        return result.toString().trim();
    }
}

package com.ebbinghaus.memory.app.utils;

import static com.ebbinghaus.memory.app.utils.Constants.*;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.service.MessageSourceService;
import java.time.Duration;
import java.time.LocalDateTime;

public class DateUtils {

  // for new messages
  public static LocalDateTime calculateNextExecutionTime(LocalDateTime dateTime) {
    return dateTime.plusMinutes(DEFAULT_TIME_EXECUTION);
  }

  public static LocalDateTime calculateNextExecutionTime(EMessage eMessage) {
    return eMessage
        .getNextExecutionDateTime()
        .plusMinutes(
            INTERVAL_MAP.getOrDefault(eMessage.getExecutionStep(), DEFAULT_TIME_EXECUTION));
  }

  public static String formatDuration(
      LocalDateTime start,
      LocalDateTime end,
      String languageCode,
      MessageSourceService messageSourceService) {
    if (end.isBefore(start)) {
      LocalDateTime temp = start;
      start = end;
      end = temp;
    }

    Duration duration = Duration.between(start, end);
    var totalMinutes = duration.toMinutes();

    var years = totalMinutes / MINUTES_IN_YEAR;
    totalMinutes %= MINUTES_IN_YEAR;

    var months = totalMinutes / MINUTES_IN_MONTH;
    totalMinutes %= MINUTES_IN_MONTH;

    var days = totalMinutes / MINUTES_IN_DAY;
    totalMinutes %= MINUTES_IN_DAY;

    var hours = totalMinutes / MINUTES_IN_HOUR;
    var minutes = totalMinutes % MINUTES_IN_HOUR;

    StringBuilder result = new StringBuilder();
    if (years != 0)
      result
          .append(years)
          .append(messageSourceService.getMessage("messages.execution-time.years", languageCode));
    if (months != 0)
      result
          .append(months)
          .append(messageSourceService.getMessage("messages.execution-time.months", languageCode));
    if (days != 0)
      result
          .append(days)
          .append(messageSourceService.getMessage("messages.execution-time.days", languageCode));
    if (hours != 0)
      result
          .append(hours)
          .append(messageSourceService.getMessage("messages.execution-time.hours", languageCode));
    if (minutes % 60 != 0)
      result
          .append(minutes)
          .append(messageSourceService.getMessage("messages.execution-time.minutes", languageCode));
    if (result.isEmpty())
      result.append(
          messageSourceService.getMessage("messages.execution-time.minute", languageCode));

    return result.toString().trim();
  }
}

package com.ebbinghaus.memory.app.utils;

import com.ebbinghaus.memory.app.exception.TelegramCallException;
import com.ebbinghaus.memory.app.utils.function.ThrowingRunnable;
import java.util.Optional;
import org.springframework.util.function.ThrowingSupplier;

public class ObjectUtils {

  public static final String EMPTY_STRING = "";

  public static String extractSubstringForButton(String target) {
    return Optional.ofNullable(target)
        .map(
            input -> {
              int spaceIndex = input.indexOf('\u00A0');
              return spaceIndex != -1 ? input.substring(0, spaceIndex + 1) : EMPTY_STRING;
            })
        .orElse(EMPTY_STRING);
  }

  public static String titleListString(long page, long size, long totalMessages, String target) {
    long start = page * size + 1;
    long end = Math.min(start + size - 1, totalMessages);
    return String.format(target, start, end, totalMessages);
  }

  public static <T> T doTry(ThrowingSupplier<T> func) {
    try {
      return func.get();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public static void doTry(ThrowingRunnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static <T> T doTryTgCall(ThrowingSupplier<T> func) {
    try {
      return func.get();
    } catch (Exception e) {
      throw new TelegramCallException(e.getMessage());
    }
  }
}

package com.ebbinghaus.memory.app.utils;

import org.springframework.util.function.ThrowingSupplier;

import java.util.Optional;

public class ObjectUtils {

    public static final String EMPTY_STRING = "";

    public static String extractSubstringForButton(String target) {
        return Optional.ofNullable(target)
                .map(input -> {
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
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}

package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.service.MessageSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageSourceServiceImpl implements MessageSourceService {

    private final MessageSource messageSource;

    @Override
    public String getMessage(String key, String locale) {
        String s = messageSource.getMessage(key, new Object[0], Locale.of(locale));

        return s;
    }
}

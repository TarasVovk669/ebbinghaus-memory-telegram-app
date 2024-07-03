package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.service.MessageSourceService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageSourceServiceImpl implements MessageSourceService {

  private final MessageSource messageSource;

  @Override
  public String getMessage(String key, String locale) {
    return messageSource.getMessage(key, new Object[0], Locale.of(locale));
  }
}

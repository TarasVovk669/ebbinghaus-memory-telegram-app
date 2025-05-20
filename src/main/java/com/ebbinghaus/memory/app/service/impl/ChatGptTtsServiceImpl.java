package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.service.TtsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;

@Service
@RequiredArgsConstructor
public class ChatGptTtsServiceImpl implements TtsService {

  private static final Logger log = LoggerFactory.getLogger(ChatGptTtsServiceImpl.class);

  private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

  @Override
  public byte[] synthesize(String text) {
    log.info("Synthesizing text {}", text);

    return openAiAudioSpeechModel.call(text);
  }
}

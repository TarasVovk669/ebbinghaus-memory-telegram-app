package com.ebbinghaus.memory.app.config;

import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiSpeechConfig {

  @Bean
  public OpenAiAudioSpeechModel speechModel(@Value("${spring.ai.openai.api-key}") String apiKey) {
    OpenAiAudioSpeechOptions opts =
        OpenAiAudioSpeechOptions.builder()
            .withModel("tts-1")
            .withVoice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
            .withResponseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
            .withSpeed(1.0f)
            .build();
    return new OpenAiAudioSpeechModel(new OpenAiAudioApi(apiKey), opts);
  }
}

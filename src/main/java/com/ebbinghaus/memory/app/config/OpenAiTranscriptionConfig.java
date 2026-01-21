package com.ebbinghaus.memory.app.config;

import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiTranscriptionConfig {

  @Bean
  public OpenAiAudioTranscriptionModel transcriptionModel(
      @Value("${spring.ai.openai.api-key}") String apiKey) {
    var options =
        OpenAiAudioTranscriptionOptions.builder()
            .model(OpenAiAudioApi.WhisperModel.WHISPER_1.getValue())
            .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON)
            .temperature(0.2f)
            .build();

    return new OpenAiAudioTranscriptionModel(
        OpenAiAudioApi.builder().apiKey(apiKey).build(), options);
  }
}

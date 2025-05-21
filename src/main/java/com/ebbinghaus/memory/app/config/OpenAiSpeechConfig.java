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
        var opts =
                OpenAiAudioSpeechOptions.builder()
                        .model("tts-1")
                        .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                        .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                        .speed(1.0f)
                        .build();
        return new OpenAiAudioSpeechModel(OpenAiAudioApi.builder()
                .apiKey(apiKey)
                .build(), opts);
    }
}

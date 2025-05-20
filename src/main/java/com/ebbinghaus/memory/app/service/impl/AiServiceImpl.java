package com.ebbinghaus.memory.app.service.impl;

import static com.ebbinghaus.memory.app.model.QuizManageStatus.*;
import static com.ebbinghaus.memory.app.utils.Constants.PROMPT;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static java.time.ZoneOffset.UTC;

import com.ebbinghaus.memory.app.domain.quiz.QuizQuestion;
import com.ebbinghaus.memory.app.model.AiQuestionTuple;
import com.ebbinghaus.memory.app.model.QuestionsWrapper;
import com.ebbinghaus.memory.app.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

  private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

  private final OpenAiChatModel openAiChatModel;
  private final ObjectMapper objectMapper;

  @Value("${app.max-retry:3}")
  private Long maxRetries;

  @Value("${app.pause-retry:500}")
  private Long pauseBetweenRetries;

  @Override
  public AiQuestionTuple sendRequest(String text, String languageCode) {
    var format = String.format(PROMPT, languageCode, text);
    QuestionsWrapper wrapper = null;

    for (int attempt = 0; attempt < maxRetries; attempt++) {
      try {
        String response = openAiChatModel.call(format);
        log.info("Response: {}", response);
        wrapper = objectMapper.readValue(response, QuestionsWrapper.class);

        if (null != wrapper.getError() || null == wrapper.getQuestions()) {
          return new AiQuestionTuple(wrapper.getError(), null);
        }
        break; // parsing successful, break out of the retry loop
      } catch (ResourceAccessException | IOException e) {
        if (attempt >= maxRetries - 1) {
          return new AiQuestionTuple(RETRIES_LIMIT, null);
        }
        try {
          Thread.sleep(pauseBetweenRetries);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return new AiQuestionTuple(DEFAULT, null);
        }
      }
    }

    return new AiQuestionTuple(
        SUCCESS,
        wrapper.getQuestions().stream()
            .map(
                questionDto ->
                    QuizQuestion.builder()
                        .text(questionDto.getText())
                        .type(questionDto.getType())
                        .correctAnswer(questionDto.getCorrectAnswer())
                        .createdDateTime(LocalDateTime.now(UTC))
                        .variants(
                            doTry(() -> objectMapper.writeValueAsString(questionDto.getVariants())))
                        .build())
            .toList());
  }
}

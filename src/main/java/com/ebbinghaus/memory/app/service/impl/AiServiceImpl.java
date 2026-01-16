package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.quiz.QuizQuestion;
import com.ebbinghaus.memory.app.model.AiQuestionTuple;
import com.ebbinghaus.memory.app.model.QuestionsWrapper;
import com.ebbinghaus.memory.app.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static com.ebbinghaus.memory.app.model.QuizManageStatus.DEFAULT;
import static com.ebbinghaus.memory.app.model.QuizManageStatus.SUCCESS;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static java.time.ZoneOffset.UTC;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private final RetryTemplate quizJsonProcessorRetryTemplate;
    private final OpenAiChatModel openAiChatModel;
    private final ObjectMapper objectMapper;
    @Value("classpath:templates/quiz-prompt.st")
    private Resource quizPrompt;

    @Override
    @RateLimiter(name = "openai")
    public AiQuestionTuple sendRequest(String text, String languageCode) {
        var template = new PromptTemplate(quizPrompt);
        var prompt = template.create(Map.of("input_text", text,
                "language_code", languageCode));

        try {
            return quizJsonProcessorRetryTemplate.execute(context -> {
                log.info("Trying to process AI request, attempt: {}", context.getRetryCount());
                var raw = openAiChatModel.call(prompt).getResult().getOutput().getText();
                log.info("Response: {}", raw);

                var wrapper = objectMapper.readValue(raw, QuestionsWrapper.class);
                System.out.println(wrapper);

                if (wrapper.getError() != null || wrapper.getQuestions() == null) {
                    return new AiQuestionTuple(wrapper.getError(), null);
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
            });
        } catch (Exception e) {
            log.error("AI call failed", e);
            return new AiQuestionTuple(DEFAULT, null);
        }
    }
}

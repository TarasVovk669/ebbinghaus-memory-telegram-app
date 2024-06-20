package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.bot.KeyboardFactoryService;
import com.ebbinghaus.memory.app.domain.quiz.QuestionType;
import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import com.ebbinghaus.memory.app.domain.quiz.QuizQuestion;
import com.ebbinghaus.memory.app.domain.quiz.QuizStatus;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.QuizTuple;
import com.ebbinghaus.memory.app.repository.QuizQuestionRepository;
import com.ebbinghaus.memory.app.repository.QuizRepository;
import com.ebbinghaus.memory.app.service.AiService;
import com.ebbinghaus.memory.app.service.MessageSourceService;
import com.ebbinghaus.memory.app.service.QuizService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ebbinghaus.memory.app.model.QuizManageStatus.*;
import static com.ebbinghaus.memory.app.utils.Constants.MESSAGE_ID;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static java.time.ZoneOffset.UTC;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizServiceImpl.class);
    public static final int DEFAULT_QUIZ_PAGE_SIZE = 2;

    private final AiService aiService;
    private final TelegramClient telegramClient;
    private final QuizRepository quizRepository;
    private final KeyboardFactoryService factoryService;
    private final MessageSourceService messageSourceService;
    private final QuizQuestionRepository quizQuestionRepository;

    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void process(InputUserData userData) {
        log.info("Process quiz for user with id: {}", userData.getUser().getId());


        Long messageId = Long.valueOf(userData.getCallBackData().get(MESSAGE_ID));
        var quizTuple = manageUserQuiz(userData.getUser().getId(), messageId);

        switch (quizTuple.status()) {
            case MAX_PER_MESSAGE:
            case MAX_PER_DAY_LIMIT_STATUS: {
                doTry(() -> telegramClient.execute(
                        EditMessageText.builder()
                                .chatId(userData.getChatId())
                                .messageId(userData.getMessageId())
                                .text(messageSourceService.getMessage(
                                        String.format("quiz.error.%s", quizTuple.status().toString().toLowerCase()),
                                        userData.getLanguageCode()))
                                .replyMarkup(factoryService.getSingleBackFullMessageKeyboard(
                                        userData.getLanguageCode(),
                                        messageId))
                                .build()));
                break;
            }
            case SUCCESS: {
                var quiz = quizTuple.quiz();
                log.info("Get last question from quiz by id: {}", quiz.getId());
                quizQuestionRepository.findFirstByQuizIdAndStatusIsNullOrderById(quiz.getId())
                        .ifPresentOrElse(qq -> {
                                    //todo: get select from qq to get some add info
                                    //1)total question
                                    //2)answered question
                                    //3)correct question
                                    var result = quizQuestionRepository.findQuizStatisticsByQuizId(quiz.getId());

                                    System.out.println(result.getTotalQuestions());
                                    System.out.println(result.getAnsweredQuestions());
                                    System.out.println(result.getCorrectQuestions());

                                    doTry(() -> telegramClient.execute(
                                            EditMessageText.builder()
                                                    .chatId(userData.getChatId())
                                                    .messageId(userData.getMessageId())
                                                    .text(qq.getText())
                                                    .replyMarkup(factoryService.getQuizQuestionKeyboard(qq, quiz.getMessageId(), userData.getLanguageCode())) // add button
                                                    .build()));
                                },
                                () -> doTry(() -> telegramClient.execute(
                                        EditMessageText.builder()
                                                .chatId(userData.getChatId())
                                                .messageId(userData.getMessageId())
                                                .text(messageSourceService.getMessage("quiz.error.finished_quiz", userData.getLanguageCode()))
                                                .replyMarkup(factoryService.getSingleBackFullMessageKeyboard(userData.getLanguageCode(), messageId))
                                                .build())));
                break;
            }
            default:
                break;
        }
    }

    //one quiz per 24 hours on  concrete message,
    //2 quizzes per 24 hours
    public QuizTuple manageUserQuiz(Long userId, Long messageId) {
        return Optional.ofNullable(quizRepository.getFirstByOwnerIdAndMessageIdOrderByIdDesc(userId, messageId))
                .map(lastQuiz -> handleExistingQuiz(lastQuiz, messageId, userId))
                .orElseGet(() -> handleNewQuiz(userId, messageId));
    }

    private QuizTuple handleExistingQuiz(Quiz lastQuiz, Long messageId, Long userId) {
        switch (lastQuiz.getStatus()) {
            case CREATED:
            case IN_PROGRESS:
                log.info("Returning existing quiz for message: {}", messageId);
                return new QuizTuple(SUCCESS, lastQuiz);
            case FINISHED:
                if (Duration.between(lastQuiz.getFinishedDateTime().toInstant(UTC), Instant.now()).toHours() < 24) {
                    return new QuizTuple(MAX_PER_MESSAGE, null);
                } else {
                    return createQuiz(userId, messageId);
                }
            default:
                throw new IllegalStateException("Unexpected quiz status: " + lastQuiz.getStatus());
        }
    }

    private QuizTuple handleNewQuiz(Long userId, Long messageId) {
        LocalDateTime cutoffDateTime = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        var lastUserQuizzes = quizRepository.findAllRecentQuizzesByUserId(userId, cutoffDateTime);

        if (lastUserQuizzes.size() >= DEFAULT_QUIZ_PAGE_SIZE) {
            return new QuizTuple(MAX_PER_DAY_LIMIT_STATUS, null);
        }

        return createQuiz(userId, messageId);
    }

    private QuizTuple createQuiz(Long userId, Long messageId) {
        var createdQuiz = quizRepository.save(getQuizFromAI(userId, messageId));
        log.info("Created new quiz: {} for message_id: {}", createdQuiz, messageId);
        return new QuizTuple(SUCCESS, createdQuiz);
    }


    //todo: get quiz from ai and validate by schema
    private Quiz getQuizFromAI(Long userId, Long messageId) {
        //aiService.getNewQuiz();
        var mockQ = Quiz.builder()
                .status(QuizStatus.CREATED)
                .ownerId(userId)
                .messageId(messageId)
                .createdDateTime(LocalDateTime.now(UTC))
                .questions(List.of(
                        QuizQuestion.builder()
                                .type(QuestionType.SELECT)
                                .text("SELECT FROMMMMMMMMMMDMDMDDMMDMDMD")
                                .correctAnswer("true")
                                .createdDateTime(LocalDateTime.now(UTC))
                                .variants(doTry(() -> objectMapper.writeValueAsString(Map.ofEntries(
                                        Map.entry("A", "fiirst val"),
                                        Map.entry("B", "SECOND val"),
                                        Map.entry("DD", "LAST val"),
                                        Map.entry("D", "LAST val")
                                ))))
                                .build(),
                        QuizQuestion.builder()
                                .type(QuestionType.MISSING)
                                .text("MISISNGGGGNGNGNGNGNGGNGNGNGNGN")
                                .correctAnswer("true")
                                .createdDateTime(LocalDateTime.now(UTC))
                                .variants(doTry(() -> objectMapper.writeValueAsString(Map.ofEntries(
                                        Map.entry("A", "fiirst val"),
                                        Map.entry("B", "SECOND val"),
                                        Map.entry("C", "THIRT val"),
                                        Map.entry("D", "LAST val")
                                ))))
                                .build()))
                .build();
        return mockQ;
    }

    @Override
    public Quiz createQuiz() {
        return null;
    }


}

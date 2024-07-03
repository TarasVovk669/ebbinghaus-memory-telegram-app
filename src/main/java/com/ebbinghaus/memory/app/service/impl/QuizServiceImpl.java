package com.ebbinghaus.memory.app.service.impl;

import static com.ebbinghaus.memory.app.domain.quiz.QuestionType.YES_NO;
import static com.ebbinghaus.memory.app.model.QuizManageStatus.*;
import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static java.time.ZoneOffset.UTC;

import com.ebbinghaus.memory.app.domain.quiz.QuestionStatus;
import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import com.ebbinghaus.memory.app.domain.quiz.QuizStatus;
import com.ebbinghaus.memory.app.model.AiQuestionTuple;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.QuizCount;
import com.ebbinghaus.memory.app.model.QuizTuple;
import com.ebbinghaus.memory.app.repository.QuizQuestionRepository;
import com.ebbinghaus.memory.app.repository.QuizRepository;
import com.ebbinghaus.memory.app.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

  private static final Logger log = LoggerFactory.getLogger(QuizServiceImpl.class);
  public static final int DEFAULT_QUIZ_PAGE_SIZE = 2;

  private final AiService aiService;
  private final ObjectMapper objectMapper;
  private final TelegramClientService telegramClientService;
  private final QuizRepository quizRepository;
  private final MessageService messageService;
  private final KeyboardServiceImpl factoryService;
  private final MessageSourceService messageSourceService;
  private final QuizQuestionRepository quizQuestionRepository;

  @Override
  @Async
  public void process(InputUserData userData) {
    log.info("Process quiz for user with id: {}", userData.getUser().getId());

    var messageId = Long.valueOf(userData.getCallBackData().get(MESSAGE_ID));
    var quizTuple =
        manageUserQuiz(userData.getUser().getId(), messageId, userData.getLanguageCode());

    switch (quizTuple.status()) {
      case BAD_QUESTION_CANT_UNDERSTAND:
      case RETRIES_LIMIT:
      case DEFAULT:
      case TOO_SHORT:
      case MAX_PER_MESSAGE:
      case MAX_PER_DAY_LIMIT_STATUS:
        {
          doTry(
              () ->
                  telegramClientService.sendEditMessage(
                      EditMessageText.builder()
                          .chatId(userData.getChatId())
                          .messageId(userData.getMessageId())
                          .text(
                              messageSourceService.getMessage(
                                  String.format(
                                      "quiz.error.%s", quizTuple.status().toString().toLowerCase()),
                                  userData.getLanguageCode()))
                          .replyMarkup(
                              factoryService.getSingleBackFullMessageKeyboard(
                                  userData.getLanguageCode(), messageId))
                          .build()));
          break;
        }
      case SUCCESS:
        {
          var quiz = quizTuple.quiz();
          log.info("Get last question from quiz by id: {}", quiz.getId());
          manageQuizQuestion(userData, quiz.getId(), quiz.getMessageId());
          break;
        }
      default:
        break;
    }
  }

  @Override
  public void answeredQuestion(InputUserData userData) {
    var messageId = Long.valueOf(userData.getCallBackData().get(MESSAGE_ID));
    var quizQuestionId = Long.valueOf(userData.getCallBackData().get(QUIZ_QUESTION_ID));
    var quizAnswer = userData.getCallBackData().get(QUIZ_ANSWER);

    log.info(
        "Process question with id: {} and answer: {} for message_id: {}",
        quizQuestionId,
        quizAnswer,
        messageId);

    quizQuestionRepository
        .findById(quizQuestionId)
        .ifPresentOrElse(
            qq -> {
              var isCorrect = qq.getCorrectAnswer().equalsIgnoreCase(quizAnswer);

              if (null == qq.getStatus()) {
                quizQuestionRepository.save(
                    qq.setStatus(isCorrect ? QuestionStatus.CORRECT : QuestionStatus.FAILED)
                        .setUserAnswer(quizAnswer)
                        .setFinishedDateTime(LocalDateTime.now(UTC)));
                if (isCorrect) {
                  getNextQuestion(userData, qq.getQuizId());
                } else {
                  String correctAnswer;
                  if (qq.getType().equals(YES_NO)) {
                    correctAnswer =
                        messageSourceService.getMessage(
                            qq.getCorrectAnswer().equalsIgnoreCase("true")
                                ? "messages.delete.confirmation.yes"
                                : "messages.delete.confirmation.no",
                            userData.getLanguageCode());
                  } else {
                    var map = doTry(() -> objectMapper.readValue(qq.getVariants(), MAP_TYPE_REF));
                    if (map.containsKey(qq.getCorrectAnswer())) {
                      correctAnswer =
                          String.format(
                              "%s: %s", qq.getCorrectAnswer(), map.get(qq.getCorrectAnswer()));
                    } else {
                      correctAnswer = qq.getCorrectAnswer();
                    }
                  }

                  doTry(
                      () ->
                          telegramClientService.sendEditMessage(
                              EditMessageText.builder()
                                  .chatId(userData.getChatId())
                                  .messageId(userData.getMessageId())
                                  .text(
                                      String.format(
                                          messageSourceService.getMessage(
                                              "messages.quiz.incorrect_answer",
                                              userData.getLanguageCode()),
                                          correctAnswer))
                                  .replyMarkup(
                                      factoryService.getIncorrectQuizKeyboard(
                                          userData.getLanguageCode(), messageId, qq.getQuizId()))
                                  .parseMode("markdown")
                                  .build()));
                }
              } else {
                doTry(
                    () ->
                        telegramClientService.sendEditMessage(
                            EditMessageText.builder()
                                .chatId(userData.getChatId())
                                .messageId(userData.getMessageId())
                                .text(
                                    messageSourceService.getMessage(
                                        "quiz.error.already_answered", userData.getLanguageCode()))
                                .replyMarkup(
                                    factoryService.getSingleBackFullMessageKeyboard(
                                        userData.getLanguageCode(), messageId))
                                .parseMode("markdown")
                                .build()));
              }
            },
            () ->
                doTry(
                    () ->
                        telegramClientService.sendEditMessage(
                            EditMessageText.builder()
                                .chatId(userData.getChatId())
                                .messageId(userData.getMessageId())
                                .text(
                                    messageSourceService.getMessage(
                                        "quiz.error.not_found", userData.getLanguageCode()))
                                .replyMarkup(
                                    factoryService.getSingleBackFullMessageKeyboard(
                                        userData.getLanguageCode(), messageId))
                                .build())));
  }

  @Override
  public void getNextQuestion(InputUserData userData, Long quizId) {
    var messageId = Long.valueOf(userData.getCallBackData().get(MESSAGE_ID));
    var selectedQuizId =
        null != quizId ? quizId : Long.valueOf(userData.getCallBackData().get(QUIZ_ID));

    manageQuizQuestion(userData, selectedQuizId, messageId);
  }

  @Override
  public QuizCount countQuizzes(Long id) {
    log.info("Get count quizzes for user_id: {}", id);

    var now = LocalDateTime.now(UTC);
    var cutoffDateTime = now.minusHours(24);
    var quizCount = quizRepository.getQuizCount(id, cutoffDateTime, now);

    var availableQuizCount = DEFAULT_QUIZ_PAGE_SIZE - quizCount.getAvailableQuizCount();
    return new QuizCount(
        availableQuizCount, quizCount.getTotalFinishedQuizCount(), DEFAULT_QUIZ_PAGE_SIZE);
  }

  // one quiz per 24 hours on concrete message,
  // 2 quizzes per 24 hours
  private QuizTuple manageUserQuiz(Long userId, Long messageId, String languageCode) {
    return Optional.ofNullable(
            quizRepository.getFirstByOwnerIdAndMessageIdOrderByIdDesc(userId, messageId))
        .map(lastQuiz -> handleExistingQuiz(lastQuiz, messageId, userId, languageCode))
        .orElseGet(() -> handleNewQuiz(userId, messageId, languageCode));
  }

  private QuizTuple handleExistingQuiz(
      Quiz lastQuiz, Long messageId, Long userId, String languageCode) {
    switch (lastQuiz.getStatus()) {
      case CREATED:
      case IN_PROGRESS:
        log.info("Returning existing quiz for message: {}", messageId);
        return new QuizTuple(SUCCESS, lastQuiz);
      case FINISHED:
        if (Duration.between(lastQuiz.getFinishedDateTime().toInstant(UTC), Instant.now()).toHours()
            < 24) {
          return new QuizTuple(MAX_PER_MESSAGE, null);
        } else {
          return createQuiz(userId, messageId, languageCode);
        }
      default:
        throw new IllegalStateException("Unexpected quiz status: " + lastQuiz.getStatus());
    }
  }

  private QuizTuple handleNewQuiz(Long userId, Long messageId, String languageCode) {
    LocalDateTime now = LocalDateTime.now(UTC);
    LocalDateTime cutoffDateTime = now.minusHours(24);
    var quizzesCount = quizRepository.findAllRecentQuizzesByUserId(userId, cutoffDateTime, now);

    System.out.println(quizzesCount);
    if (quizzesCount >= DEFAULT_QUIZ_PAGE_SIZE) {
      return new QuizTuple(MAX_PER_DAY_LIMIT_STATUS, null);
    }

    return createQuiz(userId, messageId, languageCode);
  }

  private QuizTuple createQuiz(Long userId, Long messageId, String languageCode) {
    var aiTuple = getQuizFromAI(messageId, languageCode);

    if (SUCCESS.equals(aiTuple.status())) {
      var createdQuiz =
          quizRepository.save(
              Quiz.builder()
                  .status(QuizStatus.CREATED)
                  .ownerId(userId)
                  .messageId(messageId)
                  .createdDateTime(LocalDateTime.now(UTC))
                  .questions(aiTuple.questions())
                  .build());
      log.info("Created new quiz: {} for message_id: {}", createdQuiz, messageId);
      return new QuizTuple(SUCCESS, createdQuiz);
    } else {
      return new QuizTuple(aiTuple.status(), null);
    }
  }

  private AiQuestionTuple getQuizFromAI(Long messageId, String languageCode) {
    var message = messageService.getMessage(messageId, false);
    return aiService.sendRequest(message.getText(), languageCode);
  }

  private void manageQuizQuestion(InputUserData userData, Long quizId, Long messageId) {
    var statistic = quizQuestionRepository.findQuizStatisticsByQuizId(quizId);
    quizQuestionRepository
        .findFirstByQuizIdAndStatusIsNullOrderById(quizId)
        .ifPresentOrElse(
            qq -> {
              var text =
                  String.format(
                      messageSourceService.getMessage(
                          "messages.quiz.question.template", userData.getLanguageCode()),
                      statistic.getAnsweredQuestions() + 1,
                      qq.getText().replaceAll("_", "\\\\_"));

              doTry(
                  () ->
                      telegramClientService.sendEditMessage(
                          EditMessageText.builder()
                              .chatId(userData.getChatId())
                              .messageId(userData.getMessageId())
                              .text(text)
                              .parseMode("markdown")
                              .replyMarkup(
                                  factoryService.getQuizQuestionKeyboard(
                                      qq, messageId, userData.getLanguageCode())) // add button
                              .build()));
            },
            () -> {
              quizRepository
                  .findById(quizId)
                  .ifPresent(
                      quiz ->
                          quizRepository.save(
                              quiz.setStatus(QuizStatus.FINISHED)
                                  .setFinishedDateTime(LocalDateTime.now(UTC))));

              doTry(
                  () ->
                      telegramClientService.sendEditMessage(
                          EditMessageText.builder()
                              .chatId(userData.getChatId())
                              .messageId(userData.getMessageId())
                              .text(
                                  String.format(
                                      messageSourceService.getMessage(
                                          "quiz.error.finished_quiz", userData.getLanguageCode()),
                                      statistic.getTotalQuestions(),
                                      statistic.getAnsweredQuestions(),
                                      statistic.getCorrectQuestions()))
                              .replyMarkup(
                                  factoryService.getSingleBackFullMessageKeyboard(
                                      userData.getLanguageCode(),
                                      messageId,
                                      "messages.quiz.finish"))
                              .build()));
            });
  }
}

package com.ebbinghaus.memory.app.service.impl;

import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;

import com.ebbinghaus.memory.app.domain.quiz.QuestionType;
import com.ebbinghaus.memory.app.domain.quiz.QuizQuestion;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.service.KeyboardService;
import com.ebbinghaus.memory.app.service.MessageSourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Service
@AllArgsConstructor
public class KeyboardServiceImpl implements KeyboardService {

  private final ObjectMapper objectMapper;
  private final MessageSourceService messageSourceService;

  @Override
  public ReplyKeyboard getMainMenuKeyboard(String languageCode) {
    KeyboardRow row1 =
        new KeyboardRow(
            List.of(
                new KeyboardButton(
                    messageSourceService.getMessage("messages.menu.add-new-info", languageCode))));
    KeyboardRow row2 =
        new KeyboardRow(
            List.of(
                new KeyboardButton(
                    messageSourceService.getMessage("messages.menu.data-list", languageCode)),
                new KeyboardButton(
                    messageSourceService.getMessage("messages.menu.category-list", languageCode)),
                new KeyboardButton(
                    messageSourceService.getMessage("messages.menu.profile", languageCode))));

    return ReplyKeyboardMarkup.builder()
        .keyboard(Arrays.asList(row1, row2))
        .resizeKeyboard(true)
        .isPersistent(true)
        .oneTimeKeyboard(false)
        .build();
  }

  @Override
  public InlineKeyboardMarkup getMessageKeyboard(Long messageId, String languageCode) {
    var rowInline = new ArrayList<InlineKeyboardButton>();

    rowInline.add(
        InlineKeyboardButton.builder()
            .text(messageSourceService.getMessage("messages.navigation.view", languageCode))
            .callbackData(
                doTry(
                    () ->
                        objectMapper.writeValueAsString(
                            Map.ofEntries(
                                Map.entry(OPERATION, VIEW_MESSAGE_CALLBACK),
                                Map.entry(MESSAGE_ID, messageId)))))
            .build());

    return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(rowInline)));
  }

  @Override
  public InlineKeyboardMarkup getViewKeyboard(
      Long messageId, String languageCode, boolean isForwardedMessage, boolean isSimpleMessage) {
    var rowInline = new ArrayList<InlineKeyboardRow>();

    rowInline.add(
        new InlineKeyboardRow(
            InlineKeyboardButton.builder()
                .text(messageSourceService.getMessage("messages.navigation.back", languageCode))
                .callbackData(
                    doTry(
                        () ->
                            objectMapper.writeValueAsString(
                                Map.ofEntries(
                                    Map.entry(OPERATION, BACK_MESSAGE_CALLBACK),
                                    Map.entry(MESSAGE_ID, messageId)))))
                .build()));

    if (isSimpleMessage) {
      rowInline.add(
          new InlineKeyboardRow(
              InlineKeyboardButton.builder()
                  .text(messageSourceService.getMessage("messages.navigation.test", languageCode))
                  .callbackData(
                      doTry(
                          () ->
                              objectMapper.writeValueAsString(
                                  Map.ofEntries(
                                      Map.entry(OPERATION, TEST_MESSAGE_CALLBACK),
                                      Map.entry(MESSAGE_ID, messageId)))))
                  .build()));
    }

    rowInline.add(
        new InlineKeyboardRow(
            InlineKeyboardButton.builder()
                .text(messageSourceService.getMessage("messages.navigation.restart", languageCode))
                .callbackData(
                    doTry(
                        () ->
                            objectMapper.writeValueAsString(
                                Map.ofEntries(
                                    Map.entry(OPERATION, RESTART_MESSAGE_CALLBACK),
                                    Map.entry(MESSAGE_ID, messageId)))))
                .build()));

    if (!isForwardedMessage) {
      rowInline.add(
          new InlineKeyboardRow(
              InlineKeyboardButton.builder()
                  .text(messageSourceService.getMessage("messages.navigation.edit", languageCode))
                  .callbackData(
                      doTry(
                          () ->
                              objectMapper.writeValueAsString(
                                  Map.ofEntries(
                                      Map.entry(OPERATION, EDIT_MESSAGE_CALLBACK),
                                      Map.entry(MESSAGE_ID, messageId)))))
                  .build()));
    }

    rowInline.add(
        new InlineKeyboardRow(
            InlineKeyboardButton.builder()
                .text(messageSourceService.getMessage("messages.navigation.delete", languageCode))
                .callbackData(
                    doTry(
                        () ->
                            objectMapper.writeValueAsString(
                                Map.ofEntries(
                                    Map.entry(OPERATION, DELETE_MESSAGE_CALLBACK),
                                    Map.entry(MESSAGE_ID, messageId)))))
                .build()));

    return new InlineKeyboardMarkup(rowInline);
  }

  @Override
  public InlineKeyboardMarkup getDeleteKeyboard(Long messageId, String languageCode) {
    return new InlineKeyboardMarkup(
        List.of(
            new InlineKeyboardRow(
                List.of(
                    InlineKeyboardButton.builder()
                        .text(
                            messageSourceService.getMessage(
                                "messages.delete.confirmation.yes", languageCode))
                        .callbackData(
                            doTry(
                                () ->
                                    objectMapper.writeValueAsString(
                                        Map.ofEntries(
                                            Map.entry(OPERATION, DELETE_MESSAGE_YES_CALLBACK),
                                            Map.entry(MESSAGE_ID, messageId)))))
                        .build(),
                    InlineKeyboardButton.builder()
                        .text(
                            messageSourceService.getMessage(
                                "messages.delete.confirmation.no", languageCode))
                        .callbackData(
                            doTry(
                                () ->
                                    objectMapper.writeValueAsString(
                                        Map.ofEntries(
                                            Map.entry(OPERATION, DELETE_MESSAGE_NO_CALLBACK),
                                            Map.entry(MESSAGE_ID, messageId)))))
                        .build()))));
  }

  @Override
  public InlineKeyboardMarkup getRestartKeyboard(Long messageId, String languageCode) {
    return new InlineKeyboardMarkup(
        List.of(
            new InlineKeyboardRow(
                List.of(
                    InlineKeyboardButton.builder()
                        .text(
                            messageSourceService.getMessage(
                                "messages.delete.confirmation.yes", languageCode))
                        .callbackData(
                            doTry(
                                () ->
                                    objectMapper.writeValueAsString(
                                        Map.ofEntries(
                                            Map.entry(OPERATION, RESTART_MESSAGE_YES_CALLBACK),
                                            Map.entry(MESSAGE_ID, messageId)))))
                        .build(),
                    InlineKeyboardButton.builder()
                        .text(
                            messageSourceService.getMessage(
                                "messages.delete.confirmation.no", languageCode))
                        .callbackData(
                            doTry(
                                () ->
                                    objectMapper.writeValueAsString(
                                        Map.ofEntries(
                                            Map.entry(OPERATION, RESTART_MESSAGE_NO_CALLBACK),
                                            Map.entry(MESSAGE_ID, messageId)))))
                        .build()))));
  }

  @Override
  public InlineKeyboardMarkup getProfileKeyboard(String languageCode) {
    var changeLanguage =
        InlineKeyboardButton.builder()
            .text(messageSourceService.getMessage("messages.profile.change-language", languageCode))
            .callbackData(
                doTry(
                    () ->
                        objectMapper.writeValueAsString(
                            Map.ofEntries(Map.entry(OPERATION, VIEW_PROFILE_LANGUAGE_CALLBACK)))))
            .build();

    var contactInfo =
        InlineKeyboardButton.builder()
            .text(messageSourceService.getMessage("messages.profile.contact-info", languageCode))
            .callbackData(
                doTry(
                    () ->
                        objectMapper.writeValueAsString(
                            Map.ofEntries(Map.entry(OPERATION, CONTACT_INFO_CALLBACK)))))
            .build();

    var howItWorks =
        InlineKeyboardButton.builder()
            .text(messageSourceService.getMessage("messages.profile.how-to-use", languageCode))
            .callbackData(
                doTry(
                    () ->
                        objectMapper.writeValueAsString(
                            Map.ofEntries(Map.entry(OPERATION, HOT_IT_WORKS_CALLBACK)))))
            .build();

    return new InlineKeyboardMarkup(
        List.of(
            new InlineKeyboardRow(changeLanguage),
            new InlineKeyboardRow(howItWorks),
            new InlineKeyboardRow(contactInfo)));
  }

  @Override
  public InlineKeyboardMarkup getAvailableLanguage(String languageCode) {
    List<InlineKeyboardRow> list =
        new ArrayList<>(
            AVAILABLE_LANGUAGES_MAP.entrySet().stream()
                .filter(e -> !e.getKey().equals(languageCode))
                .map(
                    e ->
                        List.of(
                            InlineKeyboardButton.builder()
                                .text(e.getValue().emoji().concat(" ").concat(e.getValue().name()))
                                .callbackData(
                                    doTry(
                                        () ->
                                            objectMapper.writeValueAsString(
                                                Map.ofEntries(
                                                    Map.entry(
                                                        OPERATION,
                                                        CHANGE_PROFILE_LANGUAGE_CALLBACK),
                                                    Map.entry(LANGUAGE_CODE, e.getKey())))))
                                .build()))
                .map(InlineKeyboardRow::new)
                .toList());

    list.add(
        new InlineKeyboardRow(
            List.of(
                InlineKeyboardButton.builder()
                    .text(messageSourceService.getMessage("messages.navigation.back", languageCode))
                    .callbackData(
                        doTry(
                            () ->
                                objectMapper.writeValueAsString(
                                    Map.ofEntries(
                                        Map.entry(OPERATION, PROFILE_MAIN_MENU_CALLBACK)))))
                    .build())));
    return new InlineKeyboardMarkup(list);
  }

  @Override
  public InlineKeyboardMarkup getSingleBackProfileKeyboard(String languageCode) {
    return new InlineKeyboardMarkup(
        List.of(
            new InlineKeyboardRow(
                List.of(
                    InlineKeyboardButton.builder()
                        .text(
                            messageSourceService.getMessage(
                                "messages.navigation.back", languageCode))
                        .callbackData(
                            doTry(
                                () ->
                                    objectMapper.writeValueAsString(
                                        Map.ofEntries(
                                            Map.entry(OPERATION, PROFILE_MAIN_MENU_CALLBACK)))))
                        .build()))));
  }

  @Override
  public InlineKeyboardMarkup getSingleBackFullMessageKeyboard(
      String languageCode, Long messageId) {
    return getSingleBackFullMessageKeyboard(languageCode, messageId, "messages.navigation.back");
  }

  @Override
  public InlineKeyboardMarkup getSingleBackFullMessageKeyboard(
      String languageCode, Long messageId, String messageText) {
    return new InlineKeyboardMarkup(
        List.of(
            new InlineKeyboardRow(
                List.of(
                    InlineKeyboardButton.builder()
                        .text(messageSourceService.getMessage(messageText, languageCode))
                        .callbackData(
                            doTry(
                                () ->
                                    objectMapper.writeValueAsString(
                                        Map.ofEntries(
                                            Map.entry(OPERATION, BACK_FULL_MESSAGE_CALLBACK),
                                            Map.entry(MESSAGE_ID, messageId)))))
                        .build()))));
  }

  @Override
  public InlineKeyboardMarkup getQuizQuestionKeyboard(
      QuizQuestion qq, Long messageId, String languageCode) {
    List<InlineKeyboardRow> list;
    if (qq.getType().equals(QuestionType.YES_NO)) {
      list =
          List.of(
              new InlineKeyboardRow(
                  List.of(
                      InlineKeyboardButton.builder()
                          .text(
                              messageSourceService.getMessage(
                                  "messages.delete.confirmation.yes", languageCode))
                          .callbackData(
                              doTry(
                                  () ->
                                      objectMapper.writeValueAsString(
                                          Map.ofEntries(
                                              Map.entry(OPERATION, QUIZ_QUESTION_CALLBACK),
                                              Map.entry(QUIZ_QUESTION_ID, qq.getId()),
                                              Map.entry(QUIZ_ANSWER, "true"),
                                              Map.entry(MESSAGE_ID, messageId)))))
                          .build(),
                      InlineKeyboardButton.builder()
                          .text(
                              messageSourceService.getMessage(
                                  "messages.delete.confirmation.no", languageCode))
                          .callbackData(
                              doTry(
                                  () ->
                                      objectMapper.writeValueAsString(
                                          Map.ofEntries(
                                              Map.entry(OPERATION, QUIZ_QUESTION_CALLBACK),
                                              Map.entry(QUIZ_QUESTION_ID, qq.getId()),
                                              Map.entry(QUIZ_ANSWER, "false"),
                                              Map.entry(MESSAGE_ID, messageId)))))
                          .build())),
              new InlineKeyboardRow(
                  List.of(
                      InlineKeyboardButton.builder()
                          .text(
                              messageSourceService.getMessage("messages.quiz.close", languageCode))
                          .callbackData(
                              doTry(
                                  () ->
                                      objectMapper.writeValueAsString(
                                          Map.ofEntries(
                                              Map.entry(OPERATION, BACK_FULL_MESSAGE_CALLBACK),
                                              Map.entry(MESSAGE_ID, messageId)))))
                          .build())));
    } else {
      var map = doTry(() -> objectMapper.readValue(qq.getVariants(), MAP_TYPE_REF));

      list =
          new ArrayList<>(
              map.entrySet().stream()
                  .map(
                      v ->
                          List.of(
                              InlineKeyboardButton.builder()
                                  .text(String.format("%s: %s", v.getKey(), v.getValue()))
                                  .callbackData(
                                      doTry(
                                          () ->
                                              objectMapper.writeValueAsString(
                                                  Map.ofEntries(
                                                      Map.entry(OPERATION, QUIZ_QUESTION_CALLBACK),
                                                      Map.entry(QUIZ_QUESTION_ID, qq.getId()),
                                                      Map.entry(QUIZ_ANSWER, v.getKey()),
                                                      Map.entry(MESSAGE_ID, messageId)))))
                                  .build()))
                  .map(InlineKeyboardRow::new)
                  .toList());

      list.add(
          new InlineKeyboardRow(
              List.of(
                  InlineKeyboardButton.builder()
                      .text(messageSourceService.getMessage("messages.quiz.close", languageCode))
                      .callbackData(
                          doTry(
                              () ->
                                  objectMapper.writeValueAsString(
                                      Map.ofEntries(
                                          Map.entry(OPERATION, BACK_FULL_MESSAGE_CALLBACK),
                                          Map.entry(MESSAGE_ID, messageId)))))
                      .build())));
    }

    return new InlineKeyboardMarkup(list);
  }

  @Override
  public InlineKeyboardMarkup getIncorrectQuizKeyboard(
      String languageCode, Long messageId, Long quizId) {
    var list =
        List.of(
            new InlineKeyboardRow(
                List.of(
                    InlineKeyboardButton.builder()
                        .text(messageSourceService.getMessage("messages.quiz.next", languageCode))
                        .callbackData(
                            doTry(
                                () ->
                                    objectMapper.writeValueAsString(
                                        Map.ofEntries(
                                            Map.entry(OPERATION, QUIZ_NEXT_QUESTION_CALLBACK),
                                            Map.entry(MESSAGE_ID, messageId),
                                            Map.entry(QUIZ_ID, quizId)))))
                        .build())),
            new InlineKeyboardRow(
                List.of(
                    InlineKeyboardButton.builder()
                        .text(messageSourceService.getMessage("messages.quiz.close", languageCode))
                        .callbackData(
                            doTry(
                                () ->
                                    objectMapper.writeValueAsString(
                                        Map.ofEntries(
                                            Map.entry(OPERATION, BACK_FULL_MESSAGE_CALLBACK),
                                            Map.entry(MESSAGE_ID, messageId)))))
                        .build())));

    return new InlineKeyboardMarkup(list);
  }

  @Override
  public ArrayList<InlineKeyboardButton> getNavigationButtons(
      int page, int size, Page<?> messages, String operation, InputUserData inputUserData) {
    var navigationButtons = new ArrayList<InlineKeyboardButton>();
    if (page != 0) {
      var callbackDataMap =
          new HashMap<String, Object>(
              Map.ofEntries(
                  Map.entry(OPERATION, operation),
                  Map.entry(PAGE, page - 1),
                  Map.entry(SIZE, size)));

      if (null != inputUserData.getCallBackData()
          && inputUserData.getCallBackData().containsKey(IS_BACK)) {
        callbackDataMap.put(IS_BACK, inputUserData.getCallBackData().get(IS_BACK));
        callbackDataMap.put(CATEGORY_ID, inputUserData.getCallBackData().get(CATEGORY_ID));
        callbackDataMap.put(CATEGORY_PAGE, inputUserData.getCallBackData().get(CATEGORY_PAGE));
        callbackDataMap.put(CATEGORY_SIZE, inputUserData.getCallBackData().get(CATEGORY_SIZE));
      }

      navigationButtons.add(
          InlineKeyboardButton.builder()
              .text(
                  messageSourceService.getMessage(
                      "messages.navigation.previous", inputUserData.getLanguageCode()))
              .callbackData(doTry(() -> objectMapper.writeValueAsString(callbackDataMap)))
              .build());
    }
    if (messages.getTotalPages() - 1 != page) {
      var callbackDataMap =
          new HashMap<String, Object>(
              Map.ofEntries(
                  Map.entry(OPERATION, operation),
                  Map.entry(PAGE, page + 1),
                  Map.entry(SIZE, size)));

      if (null != inputUserData.getCallBackData()
          && inputUserData.getCallBackData().containsKey(IS_BACK)) {
        callbackDataMap.put(IS_BACK, inputUserData.getCallBackData().get(IS_BACK));
        callbackDataMap.put(CATEGORY_ID, inputUserData.getCallBackData().get(CATEGORY_ID));
        callbackDataMap.put(CATEGORY_PAGE, inputUserData.getCallBackData().get(CATEGORY_PAGE));
        callbackDataMap.put(CATEGORY_SIZE, inputUserData.getCallBackData().get(CATEGORY_SIZE));
      }

      navigationButtons.add(
          InlineKeyboardButton.builder()
              .text(
                  messageSourceService.getMessage(
                      "messages.navigation.next", inputUserData.getLanguageCode()))
              .callbackData(doTry(() -> objectMapper.writeValueAsString(callbackDataMap)))
              .build());
    }
    if (null != inputUserData.getCallBackData()
        && inputUserData.getCallBackData().containsKey(IS_BACK)) {
      navigationButtons.add(
          InlineKeyboardButton.builder()
              .text(
                  messageSourceService.getMessage(
                      "messages.navigation.back", inputUserData.getLanguageCode()))
              .callbackData(
                  doTry(
                      () ->
                          objectMapper.writeValueAsString(
                              Map.ofEntries(
                                  Map.entry(OPERATION, NAVIGATION_CATEGORY_LIST_CALLBACK),
                                  Map.entry(
                                      PAGE, inputUserData.getCallBackData().get(CATEGORY_PAGE)),
                                  Map.entry(
                                      SIZE, inputUserData.getCallBackData().get(CATEGORY_SIZE))))))
              .build());
    }
    return navigationButtons;
  }
}

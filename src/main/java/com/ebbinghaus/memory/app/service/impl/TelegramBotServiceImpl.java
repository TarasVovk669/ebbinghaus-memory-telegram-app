package com.ebbinghaus.memory.app.service.impl;

import static com.ebbinghaus.memory.app.model.UserState.*;
import static com.ebbinghaus.memory.app.model.UserState.CATEGORY_DATA_LIST;
import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.Constants.HELP;
import static com.ebbinghaus.memory.app.utils.Constants.QUIZ_NEXT_QUESTION_CALLBACK;
import static com.ebbinghaus.memory.app.utils.DateUtils.calculateNextExecutionTime;
import static com.ebbinghaus.memory.app.utils.MessageUtils.*;
import static com.ebbinghaus.memory.app.utils.MessageUtils.manageMsgType;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.*;
import static java.time.ZoneOffset.UTC;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.domain.EMessageEntity;
import com.ebbinghaus.memory.app.domain.EMessageType;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.model.MessageTuple;
import com.ebbinghaus.memory.app.model.UserState;
import com.ebbinghaus.memory.app.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

@Service
public class TelegramBotServiceImpl implements TelegramBotService {

  private static final Logger log = LoggerFactory.getLogger(TelegramBotServiceImpl.class);
  private static final Map<String, Function<InputUserData, Boolean>> functionCommandMap =
      new HashMap<>();
  private static final Map<String, Function<InputUserData, Boolean>> functionCallbackDataMap =
      new HashMap<>();
  private static final Map<UserState, Function<InputUserData, Boolean>> functionUserStateMap =
      new HashMap<>();

  private Executor quizTaskExecutor;
  private QuizService quizService;
  private UserService userService;
  private ObjectMapper objectMapper;
  private MessageService messageService;
  private CategoryService categoryService;
  private KeyboardService keyboardService;
  private SchedulerService schedulerService;
  private MessageSourceService messageSourceService;
  private TelegramClientService telegramClientService;
  private ChatMessageStateService chatMessageStateService;

  public TelegramBotServiceImpl(
      @Qualifier("quizTaskExecutor") Executor quizTaskExecutor,
      QuizService quizService,
      UserService userService,
      MessageService messageService,
      CategoryService categoryService,
      ChatMessageStateService chatMessageStateService,
      ObjectMapper objectMapper,
      SchedulerService schedulerService,
      MessageSourceService messageSourceService,
      KeyboardService keyboardService,
      TelegramClientService telegramClientService) {
    this.quizTaskExecutor = quizTaskExecutor;
    this.quizService = quizService;
    this.userService = userService;
    this.objectMapper = objectMapper;
    this.messageService = messageService;
    this.categoryService = categoryService;
    this.schedulerService = schedulerService;
    this.messageSourceService = messageSourceService;
    this.keyboardService = keyboardService;
    this.chatMessageStateService = chatMessageStateService;
    this.telegramClientService = telegramClientService;

    functionCommandMap.put(START, handleStartMessage);
    functionCommandMap.put(HELP, handleHelpMessage);
    functionCommandMap.put(ADD_NEW_INFO, handleButtonAddNewInfo);
    functionCommandMap.put(INFO_LIST, handleButtonInfoList);
    functionCommandMap.put(CATEGORY_LIST, handleButtonCategoryList);
    functionCommandMap.put(PROFILE_LIST, handleProfileMainMenu);

    functionCallbackDataMap.put(VIEW_MESSAGE_CALLBACK, handleMessageView);
    functionCallbackDataMap.put(VIEW_SHORT_MESSAGE_CALLBACK, handleShortMessageView);
    functionCallbackDataMap.put(EDIT_MESSAGE_CALLBACK, handleMessageEdit);
    functionCallbackDataMap.put(DELETE_MESSAGE_CALLBACK, handleMessageDelete);
    functionCallbackDataMap.put(RESTART_MESSAGE_CALLBACK, handleMessageRestart);
    functionCallbackDataMap.put(TEST_MESSAGE_CALLBACK, handleTestMessage);
    functionCallbackDataMap.put(BACK_MESSAGE_CALLBACK, handleMessageBack);
    functionCallbackDataMap.put(BACK_FULL_MESSAGE_CALLBACK, handleFullMessageBack);
    functionCallbackDataMap.put(NAVIGATION_DATA_LIST_CALLBACK, handleButtonInfoList);
    functionCallbackDataMap.put(NAVIGATION_CATEGORY_LIST_CALLBACK, handleButtonCategoryList);
    functionCallbackDataMap.put(EDIT_CONCRETE_MESSAGE_CALLBACK, handleEditConcreteMessage);
    functionCallbackDataMap.put(DELETE_MESSAGE_YES_CALLBACK, handleMessageDeleteYes);
    functionCallbackDataMap.put(RESTART_MESSAGE_YES_CALLBACK, handleMessageRestartYes);
    functionCallbackDataMap.put(DELETE_MESSAGE_NO_CALLBACK, handleMessageNoAction);
    functionCallbackDataMap.put(RESTART_MESSAGE_NO_CALLBACK, handleMessageNoAction);
    functionCallbackDataMap.put(VIEW_PROFILE_LANGUAGE_CALLBACK, handleMessageChangeLanguage);
    functionCallbackDataMap.put(HOT_IT_WORKS_CALLBACK, howItWorksCallback);
    functionCallbackDataMap.put(PROFILE_MAIN_MENU_CALLBACK, handleProfileMainMenuBack);
    functionCallbackDataMap.put(CHANGE_PROFILE_LANGUAGE_CALLBACK, handleChangeLanguage);
    functionCallbackDataMap.put(CONTACT_INFO_CALLBACK, handleContactInfo);
    functionCallbackDataMap.put(QUIZ_QUESTION_CALLBACK, handleQuizQuestion);
    functionCallbackDataMap.put(QUIZ_NEXT_QUESTION_CALLBACK, handleQuizNextQuestion);

    functionUserStateMap.put(WAIT_TEXT, handleInputText);
    functionUserStateMap.put(WAIT_FORWARDED_MESSAGE, handleInputText);
  }

  @Override
  public void processEditMessageCallback(String command, InputUserData inputUserData) {
    functionCallbackDataMap.get(command).apply(inputUserData);
  }

  @Override
  public void processButtonMessageCallback(String command, InputUserData inputUserData) {
    functionCallbackDataMap.get(command).apply(inputUserData);
  }

  @Override
  public void processTextInputCallback(InputUserData inputUserData) {
    Optional.ofNullable(inputUserData.getMessageText())
        .ifPresentOrElse(
            messageText ->
                Optional.ofNullable(
                        messageText.startsWith("/")
                            ? functionCommandMap.get(inputUserData.getMessageText().split(" ")[0])
                            : functionCommandMap.get(
                                extractSubstringForButton(inputUserData.getMessageText())))
                    .ifPresentOrElse(
                        function -> function.apply(inputUserData),
                        () ->
                            Optional.ofNullable(functionUserStateMap.get(inputUserData.getState()))
                                .ifPresentOrElse(
                                    function -> function.apply(inputUserData),
                                    () -> manageInvalidInputMessage(inputUserData))),
            () ->
                Optional.ofNullable(functionUserStateMap.get(inputUserData.getState()))
                    .ifPresentOrElse(
                        function -> function.apply(inputUserData),
                        () -> manageInvalidInputMessage(inputUserData)));
  }

  private final Function<InputUserData, Boolean> handleStartMessage =
      userData -> {
        userService.setUserState(userData.getUser().getId(), MAIN_MENU);

        String url =
            String.format(
                messageSourceService.getMessage("messages.image.url", userData.getLanguageCode()),
                AVAILABLE_LANGUAGES_MAP.containsKey(userData.getLanguageCode())
                    ? userData.getLanguageCode()
                    : DEFAULT_LANGUAGE_CODE);

        Message photoMessage =
            telegramClientService.sendPhotoMessage(
                userData.getChatId(),
                String.format(
                    messageSourceService.getMessage(
                        "messages.greeting.start", userData.getLanguageCode()),
                    userData.getUser().getFirstName()),
                keyboardService.getMainMenuKeyboard(userData.getLanguageCode()),
                url,
                IMAGE_CACHE_MAP.get(url));

        telegramClientService.sendMessage(
            userData.getChatId(),
            String.format(
                messageSourceService.getMessage(
                    "messages.greeting.help", userData.getLanguageCode()),
                userData.getUser().getFirstName()));

        userService.addUser(userData.getUser());
        IMAGE_CACHE_MAP.putIfAbsent(url, photoMessage.getPhoto().getFirst().getFileId());
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleHelpMessage =
      userData -> {
        userService.setUserState(userData.getUser().getId(), UserState.HELP);
        clearMessages(userData, UserState.HELP);

        var message =
            telegramClientService.sendMessage(
                userData.getChatId(),
                String.format(
                    messageSourceService.getMessage(
                        "messages.help.info", userData.getLanguageCode()),
                    userData.getUser().getFirstName()),
                keyboardService.getMainMenuKeyboard(userData.getLanguageCode()));

        chatMessageStateService.addMessage(
            userData.getUser().getId(),
            userData.getChatId(),
            UserState.HELP,
            List.of(message.getMessageId()));

        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleButtonAddNewInfo =
      userData -> {
        clearMessages(userData, List.of(WAIT_TEXT, WAIT_EDIT_TEXT_CONCRETE));
        telegramClientService.deleteMessage(userData.getChatId(), userData.getMessageId());

        var message =
            telegramClientService.sendMessage(
                userData.getChatId(),
                messageSourceService.getMessage(
                    "messages.input.waiting-data", userData.getLanguageCode()));
        chatMessageStateService.addMessage(
            userData.getUser().getId(),
            userData.getChatId(),
            WAIT_TEXT,
            List.of(message.getMessageId()));

        userService.setUserState(userData.getUser().getId(), WAIT_TEXT);
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleButtonInfoList =
      userData -> {
        userService.setUserState(userData.getUser().getId(), DATA_LIST);
        clearMessages(userData, WAIT_EDIT_TEXT_CONCRETE);

        var page = getPage(userData);
        var size = getSize(userData, DEFAULT_DATA_PAGE_SIZE);
        var categoryId = getCategoryId(userData);

        var messages =
            messageService.getMessages(
                userData.getUser().getId(), categoryId, page, size, Sort.by(Sort.Order.desc("id")));

        if (messages.getTotalElements() == ZERO_COUNT) {
          telegramClientService.deleteMessage(userData.getChatId(), userData.getMessageId());
          clearMessages(userData, DATA_LIST);

          var dataInfoEmptyMessage =
              telegramClientService.sendMessage(
                  userData.getChatId(),
                  messageSourceService.getMessage(
                      "messages.collection.empty", userData.getLanguageCode()));

          chatMessageStateService.addMessage(
              userData.getUser().getId(),
              userData.getChatId(),
              DATA_LIST,
              List.of(dataInfoEmptyMessage.getMessageId()));

          return Boolean.FALSE;
        }

        var result =
            new StringBuilder(
                titleListString(
                    page,
                    size,
                    messages.getTotalElements(),
                    messageSourceService.getMessage(
                        "messages.list.title", userData.getLanguageCode())));
        var buttons = new ArrayList<InlineKeyboardButton>();

        var entities =
            new ArrayList<MessageEntity>(
                List.of(
                    MessageEntity.builder()
                        .type(BOLD_STYLE)
                        .offset(0)
                        .length(result.length())
                        .build()));

        AtomicInteger count = new AtomicInteger(page * size + 1);
        // order in for-each is important
        var suffix =
            messageSourceService.getMessage(
                "messages.suffix.execution-time", userData.getLanguageCode());

        messages.forEach(
            m -> {
              var start = result.length();
              var msgString =
                  parseMessage(
                      m,
                      false,
                      false,
                      true,
                      SHORT_ELEMENT_LENGTH,
                      suffix,
                      userData.getLanguageCode(),
                      messageSourceService);
              result.append(count.get()).append(".\n");

              entities.addAll(
                  List.of(
                      MessageEntity.builder()
                          .type(BOLD_STYLE)
                          .offset(start)
                          .length(result.length() - start)
                          .build(),
                      MessageEntity.builder()
                          .type("underline")
                          .offset(start)
                          .length(result.length() - start)
                          .build()));

              Optional.ofNullable(m.getFile())
                  .ifPresent(
                      file -> {
                        var startFile = result.length();

                        result.append(
                            messageSourceService.getMessage(
                                "file.type.".concat(file.getFileType().name().toLowerCase()),
                                userData.getLanguageCode()));
                        entities.add(
                            MessageEntity.builder()
                                .type("italic")
                                .offset(start)
                                .length(result.length() - startFile)
                                .build());
                      });

              var list =
                  getMessageEntities(m.getMessageEntities(), msgString.length(), objectMapper);
              entities.addAll(
                  list.stream()
                      .peek(me -> me.setOffset(me.getOffset() + result.length()))
                      .toList());
              result.append(msgString).append("\n\n");
              buttons.add(
                  InlineKeyboardButton.builder()
                      .text(String.valueOf(count.getAndIncrement()))
                      .callbackData(
                          doTry(
                              () ->
                                  objectMapper.writeValueAsString(
                                      Map.ofEntries(
                                          Map.entry(OPERATION, VIEW_SHORT_MESSAGE_CALLBACK),
                                          Map.entry(MESSAGE_ID, m.getId())))))
                      .build());
            });

        var navigationButtons =
            keyboardService.getNavigationButtons(
                page, size, messages, NAVIGATION_DATA_LIST_CALLBACK, userData);

        Optional.ofNullable(userData.getCallBackData())
            .ifPresentOrElse(
                cd ->
                    telegramClientService.sendEditMessage(
                        userData.getChatId(),
                        result.toString(),
                        new InlineKeyboardMarkup(
                            List.of(
                                new InlineKeyboardRow(buttons),
                                new InlineKeyboardRow(navigationButtons))),
                        entities,
                        userData.getMessageId()),
                () -> {
                  telegramClientService.deleteMessage(
                      userData.getChatId(), userData.getMessageId());
                  clearMessages(userData, DATA_LIST);

                  var msg =
                      telegramClientService.sendMessage(
                          userData.getChatId(),
                          result.toString(),
                          new InlineKeyboardMarkup(
                              List.of(
                                  new InlineKeyboardRow(buttons),
                                  new InlineKeyboardRow(navigationButtons))),
                          entities);

                  chatMessageStateService.addMessage(
                      userData.getUser().getId(),
                      userData.getChatId(),
                      DATA_LIST,
                      List.of(msg.getMessageId()));
                });
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleButtonCategoryList =
      userData -> {
        userService.setUserState(userData.getUser().getId(), CATEGORY_DATA_LIST);
        clearMessages(userData, WAIT_EDIT_TEXT_CONCRETE);

        var page = getPage(userData);
        var size = getSize(userData, DEFAULT_CATEGORY_PAGE_SIZE);

        var categories =
            categoryService.getCategories(
                userData.getUser().getId(), page, size, Sort.by(Sort.Order.desc("id")));

        if (categories.getTotalElements() == ZERO_COUNT) {
          telegramClientService.deleteMessage(userData.getChatId(), userData.getMessageId());
          clearMessages(userData, CATEGORY_DATA_LIST);

          var emptyInfoMessage =
              telegramClientService.sendMessage(
                  userData.getChatId(),
                  messageSourceService.getMessage(
                      "messages.collection.category.empty", userData.getLanguageCode()));

          chatMessageStateService.addMessage(
              userData.getUser().getId(),
              userData.getChatId(),
              CATEGORY_DATA_LIST,
              List.of(emptyInfoMessage.getMessageId()));
          return Boolean.FALSE;
        }

        var result =
            new StringBuilder(
                titleListString(
                    page,
                    size,
                    categories.getTotalElements(),
                    messageSourceService.getMessage(
                        "messages.list.title", userData.getLanguageCode())));
        var buttons = new ArrayList<InlineKeyboardButton>();

        var entities =
            new ArrayList<MessageEntity>(
                List.of(
                    MessageEntity.builder()
                        .type(BOLD_STYLE)
                        .offset(0)
                        .length(result.length())
                        .build()));

        AtomicInteger count = new AtomicInteger(page * size + 1);
        // order in for-each is important
        categories.forEach(
            c -> {
              var start = result.length();
              var msgString = c.name();
              result.append(count.get()).append(".");
              entities.addAll(
                  List.of(
                      MessageEntity.builder()
                          .type(BOLD_STYLE)
                          .offset(start)
                          .length(result.length() - start)
                          .build(),
                      MessageEntity.builder()
                          .type("underline")
                          .offset(start)
                          .length(result.length() - start)
                          .build(),
                      MessageEntity.builder()
                          .type(BOLD_STYLE)
                          .offset(result.length() + 1)
                          .length(msgString.length())
                          .build()));
              result
                  .append(" ")
                  .append(msgString)
                  .append(
                      String.format(
                          messageSourceService.getMessage(
                              c.msgQuantity() > 1
                                  ? "messages.list.quantity.many"
                                  : "messages.list.quantity.single",
                              userData.getLanguageCode()),
                          c.msgQuantity()))
                  .append("\n\n");
              buttons.add(
                  InlineKeyboardButton.builder()
                      .text(String.valueOf(count.getAndIncrement()))
                      .callbackData(
                          doTry(
                              () ->
                                  objectMapper.writeValueAsString(
                                      Map.ofEntries(
                                          Map.entry(OPERATION, NAVIGATION_DATA_LIST_CALLBACK),
                                          Map.entry(CATEGORY_ID, c.id()),
                                          Map.entry(IS_BACK, true),
                                          Map.entry(CATEGORY_PAGE, page),
                                          Map.entry(CATEGORY_SIZE, size),
                                          Map.entry(PAGE, 0),
                                          Map.entry(SIZE, DEFAULT_DATA_PAGE_SIZE)))))
                      .build());
            });

        var navigationButtons =
            keyboardService.getNavigationButtons(
                page, size, categories, NAVIGATION_CATEGORY_LIST_CALLBACK, userData);

        Optional.ofNullable(userData.getCallBackData())
            .ifPresentOrElse(
                cd ->
                    telegramClientService.sendEditMessage(
                        userData.getChatId(),
                        result.toString(),
                        new InlineKeyboardMarkup(
                            List.of(
                                new InlineKeyboardRow(buttons),
                                new InlineKeyboardRow(navigationButtons))),
                        entities,
                        userData.getMessageId()),
                () -> {
                  telegramClientService.deleteMessage(
                      userData.getChatId(), userData.getMessageId());
                  clearMessages(userData, CATEGORY_DATA_LIST);

                  var msg =
                      telegramClientService.sendMessage(
                          userData.getChatId(),
                          result.toString(),
                          new InlineKeyboardMarkup(
                              List.of(
                                  new InlineKeyboardRow(buttons),
                                  new InlineKeyboardRow(navigationButtons))),
                          entities);

                  chatMessageStateService.addMessage(
                      userData.getUser().getId(),
                      userData.getChatId(),
                      CATEGORY_DATA_LIST,
                      List.of(msg.getMessageId()));
                });
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleProfileMainMenu =
      userData -> {
        userService.setUserState(userData.getUser().getId(), PROFILE);

        clearMessages(userData, List.of(PROFILE));
        telegramClientService.deleteMessage(userData.getChatId(), userData.getMessageId());

        var messageAndCategoryCount =
            messageService.getMessageAndCategoryCount(userData.getUser().getId());
        var quizzesCount = quizService.countQuizzes(userData.getUser().getId());

        var message =
            telegramClientService.sendMessage(
                userData.getChatId(),
                String.format(
                    messageSourceService.getMessage("messages.profile", userData.getLanguageCode()),
                    userData.getUser().getFirstName(),
                    messageAndCategoryCount.getMessageCount(),
                    messageAndCategoryCount.getCategoryCount(),
                    quizzesCount.availableQuizCount(),
                    quizzesCount.totalCountPerDay(),
                    quizzesCount.totalFinishedQuizCount()),
                keyboardService.getProfileKeyboard(userData.getLanguageCode()));

        chatMessageStateService.addMessage(
            userData.getUser().getId(),
            userData.getChatId(),
            PROFILE,
            List.of(message.getMessageId()));

        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageView =
      userData -> {
        messageService
            .getMessageOptional(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true)
            .ifPresentOrElse(
                message -> {
                  var suffix =
                      messageSourceService.getMessage(
                          "messages.suffix.execution-time", userData.getLanguageCode());
                  var messageString =
                      parseMessage(
                          message, true, suffix, userData.getLanguageCode(), messageSourceService);

                  telegramClientService.sendEditMessage(
                      manageMsgType(message),
                      MessageDataRequest.builder()
                          .chatId(userData.getChatId())
                          .messageText(messageString)
                          .messageId(userData.getMessageId())
                          .entities(
                              manageMessageEntitiesLongMessage(
                                  message.getMessageEntities().stream()
                                      .map(EMessageEntity::getValue)
                                      .toList(),
                                  messageString,
                                  true,
                                  suffix,
                                  objectMapper))
                          .replyKeyboard(
                              keyboardService.getViewKeyboard(
                                  message.getId(),
                                  userData.getLanguageCode(),
                                  message.getType().equals(EMessageType.FORWARDED),
                                  null == message.getFile()
                                      || (null != message.getText()
                                          && messageString.length() >= MINIMUM_TEST_PASSED_LENGTH)))
                          .file(message.getFile())
                          .build());
                },
                () ->
                    telegramClientService.sendEditMessage(
                        userData.getMessageType(),
                        MessageDataRequest.builder()
                            .chatId(userData.getChatId())
                            .messageText(
                                messageSourceService.getMessage(
                                    "messages.error.not_found", userData.getLanguageCode()))
                            .messageId(userData.getMessageId())
                            .build()));
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleShortMessageView =
      userData -> {
        clearMessages(userData, SHORT_MESSAGE);

        messageService
            .getMessageOptional(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true)
            .ifPresentOrElse(
                message -> {
                  var suffix =
                      messageSourceService.getMessage(
                          "messages.suffix.execution-time", userData.getLanguageCode());
                  var messageString =
                      parseMessage(
                          message, false, suffix, userData.getLanguageCode(), messageSourceService);

                  var msg =
                      telegramClientService.sendMessage(
                          manageMsgType(message),
                          MessageDataRequest.builder()
                              .chatId(userData.getChatId())
                              .messageText(messageString)
                              .entities(
                                  manageMessageEntitiesShortMessage(
                                      message.getMessageEntities(),
                                      messageString,
                                      SHORT_MESSAGE_SYMBOL_QUANTITY,
                                      suffix,
                                      objectMapper))
                              .replyKeyboard(
                                  keyboardService.getMessageKeyboard(
                                      message.getId(), userData.getLanguageCode()))
                              .file(message.getFile())
                              .build());

                  chatMessageStateService.addMessage(
                      userData.getUser().getId(),
                      userData.getChatId(),
                      SHORT_MESSAGE,
                      List.of(msg.getMessageId()));
                },
                () ->
                    telegramClientService.sendEditMessage(
                        userData.getMessageType(),
                        MessageDataRequest.builder()
                            .chatId(userData.getChatId())
                            .messageText(
                                messageSourceService.getMessage(
                                    "messages.error.not_found", userData.getLanguageCode()))
                            .messageId(userData.getMessageId())
                            .build()));
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageEdit =
      userData -> {
        clearMessages(userData, WAIT_EDIT_TEXT);

        var msg =
            telegramClientService.sendMessage(
                userData.getChatId(),
                messageSourceService.getMessage("messages.edit", userData.getLanguageCode()),
                null,
                null,
                messageService
                    .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true)
                    .getMessageId());

        chatMessageStateService.addMessage(
            userData.getUser().getId(),
            userData.getChatId(),
            WAIT_EDIT_TEXT_CONCRETE,
            List.of(userData.getMessageId()));
        chatMessageStateService.addMessage(
            userData.getUser().getId(),
            userData.getChatId(),
            WAIT_EDIT_TEXT,
            List.of(msg.getMessageId()));
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageDelete =
      userData -> {
        var messageId = Long.valueOf(userData.getCallBackData().get(MESSAGE_ID));

        telegramClientService.sendEditMessage(
            userData.getMessageType(),
            MessageDataRequest.builder()
                .chatId(userData.getChatId())
                .messageText(
                    messageSourceService.getMessage(
                        "messages.delete.confirmation", userData.getLanguageCode()))
                .messageId(userData.getMessageId())
                .entities(List.of())
                .replyKeyboard(
                    keyboardService.getDeleteKeyboard(messageId, userData.getLanguageCode()))
                .file(userData.getFile())
                .build());
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageRestart =
      userData -> {
        telegramClientService.sendEditMessage(
            userData.getMessageType(),
            MessageDataRequest.builder()
                .chatId(userData.getChatId())
                .messageText(
                    messageSourceService.getMessage(
                        "messages.restart.confirmation", userData.getLanguageCode()))
                .messageId(userData.getMessageId())
                .entities(List.of())
                .replyKeyboard(
                    keyboardService.getRestartKeyboard(
                        Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)),
                        userData.getLanguageCode()))
                .file(userData.getFile())
                .build());
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleTestMessage =
      userData -> {
        telegramClientService.sendEditMessage(
            userData.getMessageType(),
            MessageDataRequest.builder()
                .chatId(userData.getChatId())
                .messageText(
                    messageSourceService.getMessage(
                        "messages.test.init", userData.getLanguageCode()))
                .messageId(userData.getMessageId())
                .build());

        quizTaskExecutor.execute(() -> quizService.process(userData));
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageBack =
      userData -> {
        var message =
            messageService.getMessage(
                Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);
        sendMessageBack(userData, message, false);
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleFullMessageBack =
      userData -> {
        var message =
            messageService.getMessage(
                Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);
        sendMessageBack(userData, message, true);
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageDeleteYes =
      userData -> {
        messageService
            .getMessageOptional(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), false)
            .ifPresentOrElse(
                m -> {
                  telegramClientService.deleteMessage(
                      userData.getChatId(), userData.getMessageId(), userData.getLanguageCode(), m);

                  messageService.deleteMessage(m.getId(), userData.getChatId());
                },
                () ->
                    telegramClientService.sendEditMessage(
                        userData.getMessageType(),
                        MessageDataRequest.builder()
                            .chatId(userData.getChatId())
                            .messageText(
                                messageSourceService.getMessage(
                                    "messages.error.not_found", userData.getLanguageCode()))
                            .messageId(userData.getMessageId())
                            .build()));

        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageRestartYes =
      userData -> {
        var message =
            messageService.restartMessageAndSchedule(
                Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), userData.getChatId());
        sendMessageBack(userData, message, false);
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageNoAction =
      userData -> {
        var message =
            messageService.getMessage(
                Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);

        sendMessageBack(userData, message, false);
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleMessageChangeLanguage =
      userData -> {
        var user = userService.getUser(userData.getUser().getId());

        telegramClientService.sendEditMessage(
            userData.getChatId(),
            String.format(
                messageSourceService.getMessage(
                    "messages.profile.language", user.getLanguageCode()),
                AVAILABLE_LANGUAGES_MAP.get(user.getLanguageCode()).emoji()),
            keyboardService.getAvailableLanguage(user.getLanguageCode()),
            null,
            userData.getMessageId());
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> howItWorksCallback =
      userData -> {
        telegramClientService.sendEditMessage(
            userData.getChatId(),
            String.format(
                messageSourceService.getMessage("messages.help.info", userData.getLanguageCode()),
                userData.getUser().getFirstName()),
            keyboardService.getSingleBackProfileKeyboard(userData.getLanguageCode()),
            null,
            userData.getMessageId());
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleEditConcreteMessage =
      userData -> {
        clearMessages(userData, List.of(WAIT_EDIT_TEXT, WAIT_EDIT_TEXT_CONCRETE));
        var editedMessage =
            messageService.updateMessage(
                prepareEditedMessage(
                    messageService.getMessageByTgExternalId(
                        userData.getMessageId().longValue(), userData.getUser().getId()),
                    userData));

        var suffix =
            messageSourceService.getMessage(
                "messages.suffix.execution-time", userData.getLanguageCode());
        var messageString =
            parseMessage(
                editedMessage, false, suffix, userData.getLanguageCode(), messageSourceService);

        telegramClientService.sendMessage(
            manageMsgType(editedMessage),
            MessageDataRequest.builder()
                .chatId(userData.getChatId())
                .messageText(messageString)
                .entities(
                    manageMessageEntitiesShortMessage(
                        editedMessage.getMessageEntities(),
                        messageString,
                        SHORT_MESSAGE_SYMBOL_QUANTITY,
                        suffix,
                        objectMapper))
                .replyKeyboard(
                    keyboardService.getMessageKeyboard(
                        editedMessage.getId(), userData.getLanguageCode()))
                .file(editedMessage.getFile())
                .build());
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleProfileMainMenuBack =
      userData -> {
        var messageAndCategoryCount =
            messageService.getMessageAndCategoryCount(userData.getUser().getId());
        var quizzesCount = quizService.countQuizzes(userData.getUser().getId());

        telegramClientService.sendEditMessage(
            userData.getChatId(),
            String.format(
                messageSourceService.getMessage("messages.profile", userData.getLanguageCode()),
                userData.getUser().getFirstName(),
                messageAndCategoryCount.getMessageCount(),
                messageAndCategoryCount.getCategoryCount(),
                quizzesCount.availableQuizCount(),
                quizzesCount.totalCountPerDay(),
                quizzesCount.totalFinishedQuizCount()),
            keyboardService.getProfileKeyboard(userData.getLanguageCode()),
            null,
            userData.getMessageId());
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleChangeLanguage =
      userData -> {
        telegramClientService.deleteMessage(userData.getChatId(), userData.getMessageId());

        var newLanguageCode = userData.getCallBackData().get(LANGUAGE_CODE);
        userService.updateLanguageCode(userData.getUser().getId(), newLanguageCode);

        telegramClientService.sendMessage(
            userData.getChatId(),
            String.format(
                messageSourceService.getMessage("messages.profile.success-change", newLanguageCode),
                userData.getUser().getFirstName()),
            keyboardService.getMainMenuKeyboard(newLanguageCode));

        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleContactInfo =
      userData -> {
        var text =
            String.format(
                messageSourceService.getMessage(
                    "messages.profile.contact-info.text", userData.getLanguageCode()),
                userData.getOwnerName());
        telegramClientService.sendEditMessage(
            userData.getChatId(),
            text,
            keyboardService.getSingleBackProfileKeyboard(userData.getLanguageCode()),
            List.of(
                MessageEntity.builder()
                    .type("mention")
                    .offset(text.indexOf(userData.getOwnerName()))
                    .length(userData.getOwnerName().length())
                    .build()),
            userData.getMessageId());
        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleQuizQuestion =
      userData -> {
        quizService.answeredQuestion(userData);

        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleQuizNextQuestion =
      userData -> {
        quizService.getNextQuestion(userData, null);

        return Boolean.TRUE;
      };

  private final Function<InputUserData, Boolean> handleInputText =
      userData -> {
        if (null != userData.getMessageText()
            && !userData.getMessageType().isAllowedSize(userData.getMessageText().length())) {
          telegramClientService.sendMessage(
              userData.getChatId(),
              messageSourceService.getMessage(
                  "messages.error.length-allowed", userData.getLanguageCode()));

          throw new RuntimeException("Message is to long for storing");
        }

        var message = messageService.addMessage(parseInput(userData));
        var suffix =
            messageSourceService.getMessage(
                "messages.suffix.execution-time", userData.getLanguageCode());
        var messageString =
            parseMessage(message, false, suffix, userData.getLanguageCode(), messageSourceService);
        clearMessages(userData, WAIT_TEXT);

        telegramClientService.sendMessage(
            userData.getMessageType(),
            MessageDataRequest.builder()
                .chatId(userData.getChatId())
                .messageText(messageString)
                .entities(
                    manageMessageEntitiesShortMessage(
                        message.getMessageEntities(),
                        messageString,
                        SHORT_MESSAGE_SYMBOL_QUANTITY,
                        suffix,
                        objectMapper))
                .replyKeyboard(
                    keyboardService.getMessageKeyboard(message.getId(), userData.getLanguageCode()))
                .file(message.getFile())
                .build());

        schedulerService.scheduleMessage(message, userData);
        userService.setUserState(userData.getUser().getId(), MAIN_MENU);
        return Boolean.TRUE;
      };

  private MessageTuple prepareEditedMessage(EMessage eMessage, InputUserData userData) {
    eMessage
        .setText(userData.getMessageText())
        .setFile(userData.getFile())
        .setMessageEntities(
            Optional.ofNullable(userData.getMessageEntities())
                .map(
                    entities ->
                        entities.stream()
                            .map(
                                me -> {
                                  try {
                                    return EMessageEntity.builder()
                                        .value(objectMapper.writeValueAsString(me))
                                        .build();
                                  } catch (JsonProcessingException e) {
                                    log.error("Error: ", e);
                                    return null;
                                  }
                                })
                            .collect(Collectors.toSet()))
                .orElse(null));

    return new MessageTuple(
        eMessage, getCategories(userData, eMessage.getType().equals(EMessageType.FORWARDED)));
  }

  private MessageTuple parseInput(InputUserData userData) {
    var input = userData.getMessageText();

    return new MessageTuple(
        EMessage.builder()
            .text(input)
            .messageEntities(
                Optional.ofNullable(userData.getMessageEntities())
                    .map(
                        entities ->
                            entities.stream()
                                .map(
                                    me -> {
                                      try {
                                        return EMessageEntity.builder()
                                            .value(objectMapper.writeValueAsString(me))
                                            .build();
                                      } catch (JsonProcessingException e) {
                                        log.error("Error: ", e);
                                        return null;
                                      }
                                    })
                                .collect(Collectors.toSet()))
                    .orElse(null))
            .ownerId(userData.getUser().getId())
            .file(userData.getFile())
            .messageId(userData.getMessageId().longValue())
            .executionStep(FIRST_EXECUTION_STEP)
            .nextExecutionDateTime(calculateNextExecutionTime(LocalDateTime.now(UTC)))
            .type(userData.isForwardedMessage() ? EMessageType.FORWARDED : EMessageType.SIMPLE)
            .build(),
        getCategories(userData, userData.isForwardedMessage()));
  }

  private void clearMessages(InputUserData userData, UserState awaitingCustomName) {
    chatMessageStateService
        .getMessages(userData.getUser().getId(), userData.getChatId(), awaitingCustomName)
        .forEach(msgId -> telegramClientService.deleteMessage(userData.getChatId(), msgId));
    chatMessageStateService.clearStateMessages(
        userData.getUser().getId(), userData.getChatId(), awaitingCustomName);
  }

  public void clearMessages(InputUserData userData, List<UserState> statuses) {
    statuses.forEach(
        s -> {
          var messages =
              chatMessageStateService.getMessages(
                  userData.getUser().getId(), userData.getChatId(), s);
          if (!messages.isEmpty()) {
            telegramClientService.deleteMessages(userData.getChatId(), messages);
          }
          chatMessageStateService.clearStateMessages(
              userData.getUser().getId(), userData.getChatId(), s);
        });
  }

  private void sendMessageBack(InputUserData userData, EMessage message, boolean isFull) {
    var suffix =
        messageSourceService.getMessage(
            "messages.suffix.execution-time", userData.getLanguageCode());
    var messageString =
        parseMessage(message, isFull, suffix, userData.getLanguageCode(), messageSourceService);

    telegramClientService.sendEditMessage(
        manageMsgType(message),
        MessageDataRequest.builder()
            .chatId(userData.getChatId())
            .messageText(messageString)
            .messageId(userData.getMessageId())
            .entities(
                isFull
                    ? manageMessageEntitiesLongMessage(
                        message.getMessageEntities().stream()
                            .map(EMessageEntity::getValue)
                            .toList(),
                        messageString,
                        true,
                        suffix,
                        objectMapper)
                    : manageMessageEntitiesShortMessage(
                        message.getMessageEntities(),
                        messageString,
                        SHORT_MESSAGE_SYMBOL_QUANTITY,
                        suffix,
                        objectMapper))
            .replyKeyboard(
                isFull
                    ? keyboardService.getViewKeyboard(
                        message.getId(),
                        userData.getLanguageCode(),
                        message.getType().equals(EMessageType.FORWARDED),
                        null == message.getFile()
                            || (null != message.getText()
                                && messageString.length() >= MINIMUM_TEST_PASSED_LENGTH))
                    : keyboardService.getMessageKeyboard(
                        message.getId(), userData.getLanguageCode()))
            .file(message.getFile())
            .build());
  }

  private void manageInvalidInputMessage(InputUserData inputUserData) {
    String key =
        inputUserData.getUser().getId().toString().concat(inputUserData.getChatId().toString());
    telegramClientService.deleteMessage(inputUserData.getChatId(), inputUserData.getMessageId());

    COUNT_MAP.compute(
        key,
        (k, count) -> {
          if (count == null) {
            return new AtomicInteger(1);
          } else {
            if (count.get() >= TRY_COUNT) {
              clearMessages(inputUserData, MESSAGES_INVALID_INPUT);
              var message =
                  telegramClientService.sendMessage(
                      inputUserData.getChatId(),
                      messageSourceService.getMessage(
                          "messages.input.invalid-text-input", inputUserData.getLanguageCode()),
                      keyboardService.getMainMenuKeyboard(inputUserData.getLanguageCode()));

              chatMessageStateService.addMessage(
                  inputUserData.getUser().getId(),
                  inputUserData.getChatId(),
                  MESSAGES_INVALID_INPUT,
                  List.of(message.getMessageId()));
              return null;
            } else {
              count.incrementAndGet();
              return count;
            }
          }
        });
  }
}

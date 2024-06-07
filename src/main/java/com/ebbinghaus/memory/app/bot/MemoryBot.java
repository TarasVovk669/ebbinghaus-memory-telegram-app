package com.ebbinghaus.memory.app.bot;

import com.ebbinghaus.memory.app.domain.*;
import com.ebbinghaus.memory.app.model.*;
import com.ebbinghaus.memory.app.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ebbinghaus.memory.app.model.UserState.*;
import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.Constants.HELP;
import static com.ebbinghaus.memory.app.utils.DateUtils.calculateNextExecutionTime;
import static com.ebbinghaus.memory.app.utils.DateUtils.formatDuration;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.*;
import static java.time.ZoneOffset.UTC;

@Component
public class MemoryBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(MemoryBot.class);


    private final String token;
    private final String ownerName;
    private final TelegramClient telegramClient;
    private final UserService userService;
    private final MessageService messageService;
    private final CategoryService categoryService;
    private final ChatMessageStateService chatMessageStateService;
    private final ObjectMapper objectMapper;
    private final SchedulerService schedulerService;
    private final MessageSourceService messageSourceService;
    private final KeyboardFactoryService keyboardFactoryService;

    private static final Map<String, Function<InputUserData, Boolean>> functionCommandMap = new HashMap<>();
    private static final Map<String, Function<InputUserData, Boolean>> functionCallbackDataMap = new HashMap<>();
    private static final Map<UserState, Function<InputUserData, Boolean>> functionUserStateMap = new HashMap<>();

    public MemoryBot(
            @Value("${bot.token}") String token,
            @Value("${bot.owner}") String ownerName,
            TelegramClient telegramClient,
            UserService userService,
            MessageService messageService,
            CategoryService categoryService,
            ChatMessageStateService chatMessageStateService,
            ObjectMapper objectMapper,
            SchedulerService schedulerService, MessageSourceService messageSourceService, KeyboardFactoryService keyboardFactoryService) {
        this.token = token;
        this.ownerName = ownerName;
        this.telegramClient = telegramClient;
        this.userService = userService;
        this.messageService = messageService;
        this.categoryService = categoryService;
        this.chatMessageStateService = chatMessageStateService;
        this.objectMapper = objectMapper;
        this.schedulerService = schedulerService;
        this.messageSourceService = messageSourceService;
        this.keyboardFactoryService = keyboardFactoryService;

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
        functionCallbackDataMap.put(BACK_MESSAGE_CALLBACK, handleMessageBack);
        functionCallbackDataMap.put(NAVIGATION_DATA_LIST_CALLBACK, handleButtonInfoList);
        functionCallbackDataMap.put(NAVIGATION_CATEGORY_LIST_CALLBACK, handleButtonCategoryList);
        functionCallbackDataMap.put(EDIT_CONCRETE_MESSAGE_CALLBACK, handleEditConcreteMessage);
        functionCallbackDataMap.put(DELETE_MESSAGE_YES_CALLBACK, handleMessageDeleteYes);
        functionCallbackDataMap.put(DELETE_MESSAGE_NO_CALLBACK, handleMessageDeleteNo);
        functionCallbackDataMap.put(VIEW_PROFILE_LANGUAGE_CALLBACK, handleMessageChangeLanguage);
        functionCallbackDataMap.put(HOT_IT_WORKS_CALLBACK, howItWorksCallback);
        functionCallbackDataMap.put(PROFILE_MAIN_MENU_CALLBACK, handleProfileMainMenuBack);
        functionCallbackDataMap.put(CHANGE_PROFILE_LANGUAGE_CALLBACK, handleChangeLanguage);
        functionCallbackDataMap.put(CONTACT_INFO_CALLBACK, handleContactInfo);

        functionUserStateMap.put(WAIT_TEXT, handleInputText);
        functionUserStateMap.put(WAIT_FORWARDED_MESSAGE, handleInputText);
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            var msgType = manageMsgType(update.getMessage());
            var isForwardedMessage = null != update.getMessage().getForwardOrigin()
                    || null != update.getMessage().getForwardFromMessageId()
                    || null != update.getMessage().getForwardDate();

            var inputUserData =
                    InputUserData.builder()
                            .messageType(msgType)
                            .chatId(update.getMessage().getChatId())
                            .user(update.getMessage().getFrom())
                            .messageId(update.getMessage().getMessageId())
                            .file(msgType.getFile(update.getMessage()))
                            .messageEntities(msgType.getMsgEntities(update.getMessage()))
                            .messageText(msgType.getMsgText(update.getMessage()))
                            .isForwardedMessage(isForwardedMessage)
                            .languageCode(userService.findUser(update.getMessage().getFrom().getId())
                                    .map(EUser::getLanguageCode)
                                    .orElse(update.getMessage().getFrom().getLanguageCode())
                            )
                            .state(isForwardedMessage
                                    ? WAIT_FORWARDED_MESSAGE
                                    : userService.getUserState(update.getMessage().getFrom().getId()))
                            .telegramClient(telegramClient)
                            .userService(userService)
                            .chatMessageStateService(chatMessageStateService)
                            .messageService(messageService)
                            .categoryService(categoryService)
                            .objectMapper(objectMapper)
                            .schedulerService(schedulerService)
                            .messageSourceService(messageSourceService)
                            .keyboardFactoryService(keyboardFactoryService)
                            .build();

            Optional.ofNullable(inputUserData.getMessageText())
                    .ifPresentOrElse(messageText -> Optional.ofNullable(
                                            messageText.startsWith("/")
                                                    ? functionCommandMap.get(inputUserData.getMessageText().split(" ")[0])
                                                    : functionCommandMap.get(extractSubstringForButton(inputUserData.getMessageText())))
                                    .ifPresentOrElse(function -> function.apply(inputUserData),
                                            () -> Optional.ofNullable(functionUserStateMap.get(inputUserData.getState()))
                                                    .ifPresentOrElse(function -> function.apply(inputUserData),
                                                            () -> manageInvalidInputMessage(inputUserData))),
                            () -> Optional.ofNullable(functionUserStateMap.get(inputUserData.getState()))
                                    .ifPresentOrElse(function -> function.apply(inputUserData),
                                            () -> manageInvalidInputMessage(inputUserData)));
        } else if (update.hasCallbackQuery()) {
            var inputMessage = (Message) update.getCallbackQuery().getMessage();
            var msgType = manageMsgType(inputMessage);
            var callBackData =
                    doTry(() -> objectMapper.readValue(update.getCallbackQuery().getData(), MAP_TYPE_REF));

            var inputUserData =
                    InputUserData.builder()
                            .messageType(msgType)
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .callBackData(callBackData)
                            .user(update.getCallbackQuery().getFrom())
                            .file(msgType.getFile(inputMessage))
                            .ownerName(ownerName)
                            .languageCode(userService.getUser(update.getCallbackQuery().getFrom().getId()).getLanguageCode())
                            .state(userService.getUserState(update.getCallbackQuery().getFrom().getId()))
                            .messageId(update.getCallbackQuery().getMessage().getMessageId())
                            .telegramClient(telegramClient)
                            .userService(userService)
                            .chatMessageStateService(chatMessageStateService)
                            .messageService(messageService)
                            .categoryService(categoryService)
                            .objectMapper(objectMapper)
                            .messageSourceService(messageSourceService)
                            .keyboardFactoryService(keyboardFactoryService)
                            .build();

            functionCallbackDataMap.get(callBackData.get(OPERATION)).apply(inputUserData);
        } else if (update.hasEditedMessage()) {
            var inputMessage = update.getEditedMessage();
            var msgType = manageMsgType(inputMessage);

            var inputUserData =
                    InputUserData.builder()
                            .messageType(msgType)
                            .messageEntities(msgType.getMsgEntities(inputMessage))
                            .messageText(msgType.getMsgText(inputMessage))
                            .chatId(inputMessage.getChatId())
                            .file(msgType.getFile(inputMessage))
                            .user(inputMessage.getFrom())
                            .ownerName(ownerName)
                            .languageCode(userService.getUser(inputMessage.getFrom().getId()).getLanguageCode())
                            .state(userService.getUserState(inputMessage.getFrom().getId()))
                            .messageId(inputMessage.getMessageId())
                            .telegramClient(telegramClient)
                            .userService(userService)
                            .chatMessageStateService(chatMessageStateService)
                            .messageService(messageService)
                            .categoryService(categoryService)
                            .objectMapper(objectMapper)
                            .messageSourceService(messageSourceService)
                            .keyboardFactoryService(keyboardFactoryService)
                            .build();

            functionCallbackDataMap.get(EDIT_CONCRETE_MESSAGE_CALLBACK).apply(inputUserData);
        }
    }

    private void manageInvalidInputMessage(InputUserData inputUserData) {
        String key = inputUserData.getUser().getId().toString().concat(inputUserData.getChatId().toString());
        deleteMessage(inputUserData.getChatId(), inputUserData.getMessageId());

        COUNT_MAP.compute(key, (k, count) -> {
            if (count == null) {
                return new AtomicInteger(1);
            } else {
                if (count.get() >= TRY_COUNT) {
                    clearMessages(inputUserData, MESSAGES_INVALID_INPUT);
                    var message = sendMessage(
                            inputUserData.getChatId(),
                            inputUserData.getMessageSourceService()
                                    .getMessage("messages.input.invalid-text-input", inputUserData.getLanguageCode()));

                    chatMessageStateService.addMessage(
                            inputUserData.getUser().getId(),
                            inputUserData.getChatId(),
                            UserState.MESSAGES_INVALID_INPUT,
                            List.of(message.getMessageId()));
                    return null;
                } else {
                    count.incrementAndGet();
                    return count;
                }
            }
        });
    }

    private MessageType manageMsgType(Message message) {
        if (message.hasText()) {
            return MessageType.SMPL;
        } else if (null != message.getPhoto() && !message.getPhoto().isEmpty()) {
            return MessageType.IMG;
        } else if (null != message.getDocument()) {
            return MessageType.DOC;
        } else if (null != message.getVideo()) {
            return MessageType.VIDEO;
        } else {
            throw new RuntimeException("Invalid msg_type");
        }
    }

    public static MessageType manageMsgType(EMessage message) {
        if (message.getFile() == null) {
            return MessageType.SMPL;
        } else if (message.getFile().getFileType().equals(FileType.PHOTO)) {
            return MessageType.IMG;
        } else if (message.getFile().getFileType().equals(FileType.DOCUMENT)) {
            return MessageType.DOC;
        } else if (message.getFile().getFileType().equals(FileType.VIDEO)) {
            return MessageType.VIDEO;
        } else {
            throw new RuntimeException("Invalid msg_type");
        }
    }

    private final Function<InputUserData, Boolean> handleStartMessage =
            userData -> {
                sendMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                "messages.greeting.start",
                                userData.getLanguageCode()), userData.getUser().getFirstName())
                );
                sendMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                "messages.greeting.help",
                                userData.getLanguageCode()), userData.getUser().getFirstName()),
                        userData.getKeyboardFactoryService().getMainMenuKeyboard(userData.getLanguageCode()));

                userData.getUserService().addUser(userData.getUser());
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleHelpMessage =
            userData -> {
                clearMessages(userData, UserState.HELP);

                var message = sendMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                "messages.help.info",
                                userData.getLanguageCode()), userData.getUser().getFirstName()),
                        userData.getKeyboardFactoryService().getMainMenuKeyboard(userData.getLanguageCode()));


                userData
                        .getChatMessageStateService()
                        .addMessage(
                                userData.getUser().getId(),
                                userData.getChatId(),
                                UserState.HELP,
                                List.of(message.getMessageId()));

                return Boolean.TRUE;
            };


    private final Function<InputUserData, Boolean> handleProfileMainMenu =
            userData -> {
                clearMessages(userData, List.of(PROFILE));
                deleteMessage(userData.getChatId(), userData.getMessageId());

                var messageAndCategoryCount = userData.getMessageService().getMessageAndCategoryCount(userData.getUser().getId());

                Message message = sendMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                        "messages.profile",
                                        userData.getLanguageCode()),
                                userData.getUser().getFirstName(),
                                messageAndCategoryCount.getMessageCount(),
                                messageAndCategoryCount.getCategoryCount()),
                        userData.getKeyboardFactoryService().getProfileKeyboard(userData.getLanguageCode()));

                userData
                        .getChatMessageStateService()
                        .addMessage(
                                userData.getUser().getId(),
                                userData.getChatId(),
                                PROFILE,
                                List.of(message.getMessageId()));

                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleProfileMainMenuBack =
            userData -> {
                var messageAndCategoryCount = userData.getMessageService().getMessageAndCategoryCount(userData.getUser().getId());

                sendEditMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                        "messages.profile",
                                        userData.getLanguageCode()),
                                userData.getUser().getFirstName(),
                                messageAndCategoryCount.getMessageCount(),
                                messageAndCategoryCount.getCategoryCount()),
                        userData.getKeyboardFactoryService().getProfileKeyboard(userData.getLanguageCode()),
                        null,
                        userData.getMessageId());
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleChangeLanguage =
            userData -> {
                deleteMessage(userData.getChatId(), userData.getMessageId());

                var newLanguageCode = userData.getCallBackData().get(LANGUAGE_CODE);
                userData.getUserService().updateLanguageCode(userData.getUser().getId(), newLanguageCode);

                sendMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                "messages.profile.success-change",
                                newLanguageCode), userData.getUser().getFirstName()),
                        userData.getKeyboardFactoryService().getMainMenuKeyboard(newLanguageCode));
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleContactInfo =
            userData -> {
                var text = String.format(userData.getMessageSourceService()
                                .getMessage("messages.profile.contact-info.text", userData.getLanguageCode()),
                        userData.getOwnerName());
                sendEditMessage(
                        userData.getChatId(),
                        text,
                        userData.getKeyboardFactoryService().getSingleBackKeyboard(userData.getLanguageCode()),
                        List.of(MessageEntity.builder()
                                .type("mention")
                                .offset(text.indexOf(userData.getOwnerName()))
                                .length(userData.getOwnerName().length())
                                .build()),
                        userData.getMessageId());
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleInputText =
            userData -> {
                if (!userData.getMessageType().isAllowedSize(userData.getMessageText().length())) {
                    sendMessage(userData.getChatId(),
                            userData.getMessageSourceService().getMessage("messages.error.length-allowed", userData.getLanguageCode()));

                    throw new RuntimeException("Message is to long for storing");
                }

                var message = userData.getMessageService().addMessage(parseInput(userData));
                var suffix = userData.getMessageSourceService().getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, false, suffix, userData.getLanguageCode(), userData.getMessageSourceService());
                clearMessages(userData, WAIT_TEXT);

                userData
                        .getMessageType()
                        .sendMessage(
                                MessageDataRequest.builder()
                                        .chatId(userData.getChatId())
                                        .messageText(messageString)
                                        .entities(
                                                manageMessageEntitiesShortMessage(
                                                        message.getMessageEntities(),
                                                        messageString,
                                                        SHORT_MESSAGE_SYMBOL_QUANTITY, suffix))
                                        .replyKeyboard(userData.getKeyboardFactoryService()
                                                .getMessageKeyboard(
                                                        message.getId(),
                                                        userData.getLanguageCode(),
                                                        userData.isForwardedMessage()))
                                        .file(message.getFile())
                                        .build(),
                                userData.getTelegramClient());

                userData.getSchedulerService().scheduleMessage(message, userData);
                userData.getUserService().setUserState(userData.getUser().getId(), MAIN_MENU);
                return Boolean.TRUE;
            };

    private List<MessageEntity> manageMessageEntitiesShortMessage(
            Collection<EMessageEntity> messageEntities, String messageString, Integer maxLength, String suffix) {
        var entities = new ArrayList<>(getMessageEntities(messageEntities, maxLength));
        entities.add(
                MessageEntity.builder()
                        .type(BOLD_STYLE)
                        .offset(messageString.lastIndexOf(suffix))
                        .length(suffix.length())
                        .build());
        return entities;
    }

    @NotNull
    private List<MessageEntity> getMessageEntities(
            Collection<EMessageEntity> messageEntities, Integer maxLength) {
        return Optional.ofNullable(messageEntities)
                .map(mes ->
                        mes.stream()
                                .map(me -> doTry(() -> objectMapper.readValue(me.getValue(), MessageEntity.class)))
                                .filter(me -> me.getOffset() <= maxLength)
                                .peek(me -> {
                                    if (me.getOffset() + me.getLength() > maxLength) {
                                        me.setLength((maxLength - me.getOffset()) + DOTS_STR.length());
                                    }
                                })
                                .toList())
                .orElse(Collections.emptyList());
    }

    private List<MessageEntity> manageMessageEntitiesLongMessage(
            Collection<String> messageEntities, String messageString, boolean addSuffix, String suffix) {
        var entities =
                new ArrayList<>(messageEntities.stream()
                        .map(me -> doTry(() -> objectMapper.readValue(me, MessageEntity.class)))
                        .toList());

        if (addSuffix) {
            entities.add(
                    MessageEntity.builder()
                            .type(BOLD_STYLE)
                            .offset(messageString.lastIndexOf(suffix))
                            .length(suffix.length())
                            .build());
        }
        return entities;
    }

    private final Function<InputUserData, Boolean> handleButtonAddNewInfo =
            userData -> {
                clearMessages(userData, List.of(WAIT_TEXT, WAIT_EDIT_TEXT_CONCRETE));
                deleteMessage(userData.getChatId(), userData.getMessageId());

                var message = sendMessage(userData.getChatId(), userData.getMessageSourceService().getMessage(
                        "messages.input.waiting-data",
                        userData.getLanguageCode()));
                userData
                        .getChatMessageStateService()
                        .addMessage(
                                userData.getUser().getId(),
                                userData.getChatId(),
                                WAIT_TEXT,
                                List.of(message.getMessageId()));

                userData.getUserService().setUserState(userData.getUser().getId(), WAIT_TEXT);
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleButtonInfoList =
            userData -> {
                clearMessages(userData, WAIT_EDIT_TEXT_CONCRETE);

                var page = getPage(userData);
                var size = getSize(userData, DEFAULT_DATA_PAGE_SIZE);
                var categoryId = getCategoryId(userData);

                var messages =
                        userData
                                .getMessageService()
                                .getMessages(
                                        userData.getUser().getId(),
                                        categoryId,
                                        page,
                                        size,
                                        Sort.by(Sort.Order.desc("id")));

                if (messages.getTotalElements() == ZERO_COUNT) {
                    sendMessage(userData.getChatId(),
                            userData
                                    .getMessageSourceService()
                                    .getMessage("messages.collection.empty", userData.getLanguageCode()));

                    return Boolean.FALSE;
                }

                var result =
                        new StringBuilder(
                                titleListString(page, size, messages.getTotalElements(), userData.getMessageSourceService().getMessage("messages.list.title", userData.getLanguageCode())));
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
                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());

                messages.forEach(
                        m -> {
                            var start = result.length();
                            var msgString = parseMessage(m, false, false, true, SHORT_ELEMENT_LENGTH, suffix, userData.getLanguageCode(), userData.getMessageSourceService());
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
                                    .ifPresent(file -> {
                                        var startFile = result.length();

                                        result.append(userData.getMessageSourceService()
                                                .getMessage("file.type.".concat(file.getFileType().name().toLowerCase()), userData.getLanguageCode()));
                                        entities.add(MessageEntity.builder()
                                                .type("italic")
                                                .offset(start)
                                                .length(result.length() - startFile)
                                                .build());
                                    });

                            entities.addAll(
                                    getMessageEntities(m.getMessageEntities(), msgString.length()).stream()
                                            .peek(me -> me.setOffset(me.getOffset() + result.length()))
                                            .toList());
                            result.append(msgString).append("\n\n");
                            buttons.add(
                                    InlineKeyboardButton.builder()
                                            .text(String.valueOf(count.getAndIncrement()))
                                            .callbackData(
                                                    doTry(
                                                            () ->
                                                                    userData
                                                                            .getObjectMapper()
                                                                            .writeValueAsString(
                                                                                    Map.ofEntries(
                                                                                            Map.entry(OPERATION, VIEW_SHORT_MESSAGE_CALLBACK),
                                                                                            Map.entry(MESSAGE_ID, m.getId())))))
                                            .build());
                        });

                var navigationButtons =
                        getNavigationButtons(page, size, messages, NAVIGATION_DATA_LIST_CALLBACK, userData);

                Optional.ofNullable(userData.getCallBackData())
                        .ifPresentOrElse(
                                cd ->
                                        sendEditMessage(
                                                userData.getChatId(),
                                                result.toString(),
                                                new InlineKeyboardMarkup(
                                                        List.of(
                                                                new InlineKeyboardRow(buttons),
                                                                new InlineKeyboardRow(navigationButtons))),
                                                entities,
                                                userData.getMessageId()),
                                () -> {
                                    deleteMessage(userData.getChatId(), userData.getMessageId());
                                    clearMessages(userData, DATA_LIST);

                                    var msg =
                                            sendMessage(
                                                    userData.getChatId(),
                                                    result.toString(),
                                                    new InlineKeyboardMarkup(
                                                            List.of(
                                                                    new InlineKeyboardRow(buttons),
                                                                    new InlineKeyboardRow(navigationButtons))),
                                                    entities);

                                    userData
                                            .getChatMessageStateService()
                                            .addMessage(
                                                    userData.getUser().getId(),
                                                    userData.getChatId(),
                                                    DATA_LIST,
                                                    List.of(msg.getMessageId()));
                                });
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleButtonCategoryList =
            userData -> {
                clearMessages(userData, WAIT_EDIT_TEXT_CONCRETE);

                var page = getPage(userData);
                var size = getSize(userData, DEFAULT_CATEGORY_PAGE_SIZE);

                var categories =
                        userData
                                .getCategoryService()
                                .getCategories(
                                        userData.getUser().getId(), page, size, Sort.by(Sort.Order.desc("id")));

                if (categories.getTotalElements() == ZERO_COUNT) {
                    sendMessage(userData.getChatId(),
                            userData
                                    .getMessageSourceService()
                                    .getMessage("messages.collection.category.empty", userData.getLanguageCode()));

                    return Boolean.FALSE;
                }

                var result =
                        new StringBuilder(
                                titleListString(page, size, categories.getTotalElements(), userData.getMessageSourceService().getMessage("messages.list.title", userData.getLanguageCode())));
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
                                            String.format(userData.getMessageSourceService().getMessage(c.msgQuantity() > 1
                                                            ? "messages.list.quantity.many"
                                                            : "messages.list.quantity.single", userData.getLanguageCode()),
                                                    c.msgQuantity()))
                                    .append("\n\n");
                            buttons.add(
                                    InlineKeyboardButton.builder()
                                            .text(String.valueOf(count.getAndIncrement()))
                                            .callbackData(
                                                    doTry(
                                                            () ->
                                                                    userData
                                                                            .getObjectMapper()
                                                                            .writeValueAsString(
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
                        getNavigationButtons(
                                page, size, categories, NAVIGATION_CATEGORY_LIST_CALLBACK, userData);

                Optional.ofNullable(userData.getCallBackData())
                        .ifPresentOrElse(
                                cd ->
                                        sendEditMessage(
                                                userData.getChatId(),
                                                result.toString(),
                                                new InlineKeyboardMarkup(
                                                        List.of(
                                                                new InlineKeyboardRow(buttons),
                                                                new InlineKeyboardRow(navigationButtons))),
                                                entities,
                                                userData.getMessageId()),
                                () -> {
                                    deleteMessage(userData.getChatId(), userData.getMessageId());
                                    clearMessages(userData, CATEGORY_DATA_LIST);

                                    var msg =
                                            sendMessage(
                                                    userData.getChatId(),
                                                    result.toString(),
                                                    new InlineKeyboardMarkup(
                                                            List.of(
                                                                    new InlineKeyboardRow(buttons),
                                                                    new InlineKeyboardRow(navigationButtons))),
                                                    entities);

                                    userData
                                            .getChatMessageStateService()
                                            .addMessage(
                                                    userData.getUser().getId(),
                                                    userData.getChatId(),
                                                    CATEGORY_DATA_LIST,
                                                    List.of(msg.getMessageId()));
                                });
                return Boolean.TRUE;
            };

    @NotNull
    private static ArrayList<InlineKeyboardButton> getNavigationButtons(
            int page, int size, Page<?> messages, String operation, InputUserData inputUserData) {
        var navigationButtons = new ArrayList<InlineKeyboardButton>();
        if (page != 0) {
            var callbackDataMap = new HashMap<String, Object>(
                    Map.ofEntries(
                            Map.entry(OPERATION, operation),
                            Map.entry(PAGE, page - 1),
                            Map.entry(SIZE, size)));

            if (null != inputUserData.getCallBackData()
                    && inputUserData.getCallBackData().containsKey(IS_BACK)) {
                callbackDataMap.put(IS_BACK, inputUserData.getCallBackData().get(IS_BACK));
                callbackDataMap.put(CATEGORY_PAGE, inputUserData.getCallBackData().get(CATEGORY_PAGE));
                callbackDataMap.put(CATEGORY_SIZE, inputUserData.getCallBackData().get(CATEGORY_SIZE));
            }

            navigationButtons.add(
                    InlineKeyboardButton.builder()
                            .text(inputUserData.getMessageSourceService().getMessage("messages.navigation.previous", inputUserData.getLanguageCode()))
                            .callbackData(
                                    doTry(
                                            () ->
                                                    inputUserData
                                                            .getObjectMapper()
                                                            .writeValueAsString(callbackDataMap)))
                            .build());
        }
        if (messages.getTotalPages() - 1 != page) {
            var callbackDataMap = new HashMap<String, Object>(
                    Map.ofEntries(
                            Map.entry(OPERATION, operation),
                            Map.entry(PAGE, page + 1),
                            Map.entry(SIZE, size)));

            if (null != inputUserData.getCallBackData()
                    && inputUserData.getCallBackData().containsKey(IS_BACK)) {
                callbackDataMap.put(IS_BACK, inputUserData.getCallBackData().get(IS_BACK));
                callbackDataMap.put(CATEGORY_PAGE, inputUserData.getCallBackData().get(CATEGORY_PAGE));
                callbackDataMap.put(CATEGORY_SIZE, inputUserData.getCallBackData().get(CATEGORY_SIZE));
            }

            navigationButtons.add(
                    InlineKeyboardButton.builder()
                            .text(inputUserData.getMessageSourceService().getMessage("messages.navigation.next", inputUserData.getLanguageCode()))
                            .callbackData(
                                    doTry(
                                            () ->
                                                    inputUserData
                                                            .getObjectMapper()
                                                            .writeValueAsString(callbackDataMap)))
                            .build());
        }
        if (null != inputUserData.getCallBackData()
                && inputUserData.getCallBackData().containsKey(IS_BACK)) {
            navigationButtons.add(
                    InlineKeyboardButton.builder()
                            .text(inputUserData.getMessageSourceService().getMessage("messages.navigation.back", inputUserData.getLanguageCode()))
                            .callbackData(
                                    doTry(
                                            () ->
                                                    inputUserData
                                                            .getObjectMapper()
                                                            .writeValueAsString(
                                                                    Map.ofEntries(
                                                                            Map.entry(OPERATION, NAVIGATION_CATEGORY_LIST_CALLBACK),
                                                                            Map.entry(PAGE, inputUserData.getCallBackData().get(CATEGORY_PAGE)),
                                                                            Map.entry(SIZE, inputUserData.getCallBackData().get(CATEGORY_SIZE))))))
                            .build());
        }
        return navigationButtons;
    }

    private static Long getCategoryId(InputUserData userData) {
        return Optional.ofNullable(userData.getCallBackData())
                .map(cd -> cd.get(CATEGORY_ID))
                .map(Long::valueOf)
                .orElse(null);
    }

    private static int getSize(InputUserData userData, Integer defaultPageSize) {
        return Optional.ofNullable(userData.getCallBackData())
                .map(cd -> cd.get(SIZE))
                .map(Integer::valueOf)
                .orElse(defaultPageSize);
    }

    private static int getPage(InputUserData userData) {
        return Optional.ofNullable(userData.getCallBackData())
                .map(cd -> cd.get(PAGE))
                .map(Integer::valueOf)
                .orElse(0);
    }

    private final Function<InputUserData, Boolean> handleMessageView =
            userData -> {
                var message =
                        userData
                                .getMessageService()
                                .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);

                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, true, suffix, userData.getLanguageCode(), userData.getMessageSourceService());

                manageMsgType(message)
                        .editMessage(
                                MessageDataRequest.builder()
                                        .chatId(userData.getChatId())
                                        .messageText(messageString)
                                        .messageId(userData.getMessageId())
                                        .entities(
                                                manageMessageEntitiesLongMessage(
                                                        message.getMessageEntities().stream()
                                                                .map(EMessageEntity::getValue).toList(), messageString, true, suffix))
                                        .replyKeyboard(userData.getKeyboardFactoryService().getViewKeyboard(
                                                message.getId(),
                                                userData.getLanguageCode(),
                                                message.getType().equals(EMessageType.FORWARDED)))
                                        .file(message.getFile())
                                        .build(),
                                userData.getTelegramClient());
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleShortMessageView =
            userData -> {
                clearMessages(userData, SHORT_MESSAGE);

                var message =
                        userData
                                .getMessageService()
                                .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);

                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, false, suffix, userData.getLanguageCode(), userData.getMessageSourceService());

                var msg =
                        manageMsgType(message)
                                .sendMessage(
                                        MessageDataRequest.builder()
                                                .chatId(userData.getChatId())
                                                .messageText(messageString)
                                                .entities(
                                                        manageMessageEntitiesShortMessage(
                                                                message.getMessageEntities(),
                                                                messageString,
                                                                SHORT_MESSAGE_SYMBOL_QUANTITY, suffix))
                                                .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(
                                                        message.getId(),
                                                        userData.getLanguageCode(),
                                                        message.getType().equals(EMessageType.FORWARDED)))
                                                .file(message.getFile())
                                                .build(),
                                        userData.getTelegramClient());

                userData
                        .getChatMessageStateService()
                        .addMessage(
                                userData.getUser().getId(),
                                userData.getChatId(),
                                SHORT_MESSAGE,
                                List.of(msg.getMessageId()));
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleMessageBack =
            userData -> {
                var message =
                        userData
                                .getMessageService()
                                .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);
                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, false,
                        suffix,
                        userData.getLanguageCode(),
                        userData.getMessageSourceService());

                manageMsgType(message)
                        .editMessage(
                                MessageDataRequest.builder()
                                        .chatId(userData.getChatId())
                                        .messageText(messageString)
                                        .messageId(userData.getMessageId())
                                        .entities(
                                                manageMessageEntitiesShortMessage(
                                                        message.getMessageEntities(),
                                                        messageString,
                                                        SHORT_MESSAGE_SYMBOL_QUANTITY, suffix))
                                        .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(
                                                message.getId(),
                                                userData.getLanguageCode(),
                                                message.getType().equals(EMessageType.FORWARDED)))
                                        .file(message.getFile())
                                        .build(),
                                userData.getTelegramClient());
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleMessageEdit =
            userData -> {
                clearMessages(userData, WAIT_EDIT_TEXT);

                var msg =
                        sendMessage(
                                userData.getChatId(),
                                userData.getMessageSourceService().getMessage("messages.edit", userData.getLanguageCode()),
                                null,
                                null,
                                userData
                                        .getMessageService()
                                        .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true)
                                        .getMessageId());

                userData
                        .getChatMessageStateService()
                        .addMessage(
                                userData.getUser().getId(),
                                userData.getChatId(),
                                WAIT_EDIT_TEXT_CONCRETE,
                                List.of(userData.getMessageId()));
                userData
                        .getChatMessageStateService()
                        .addMessage(
                                userData.getUser().getId(),
                                userData.getChatId(),
                                WAIT_EDIT_TEXT,
                                List.of(msg.getMessageId()));
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleMessageDelete =
            userData -> {
                var messageId = Long.valueOf(userData.getCallBackData().get(MESSAGE_ID));

                userData
                        .getMessageType()
                        .editMessage(
                                MessageDataRequest.builder()
                                        .chatId(userData.getChatId())
                                        .messageText(userData.getMessageSourceService().getMessage("messages.delete.confirmation", userData.getLanguageCode()))
                                        .messageId(userData.getMessageId())
                                        .entities(List.of())
                                        .replyKeyboard(userData.getKeyboardFactoryService().getDeleteKeyboard(messageId, userData.getLanguageCode()))
                                        .file(userData.getFile())
                                        .build(),
                                userData.getTelegramClient());
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleMessageDeleteYes =
            userData -> {
                deleteMessage(userData.getChatId(), userData.getMessageId());

                userData
                        .getMessageService()
                        .deleteMessage(
                                Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)),
                                userData.getChatId());
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleMessageDeleteNo =
            userData -> {
                var message =
                        userData
                                .getMessageService()
                                .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);

                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, false, suffix,
                        userData.getLanguageCode(), userData.getMessageSourceService());

                manageMsgType(message)
                        .editMessage(
                                MessageDataRequest.builder()
                                        .chatId(userData.getChatId())
                                        .messageText(messageString)
                                        .messageId(userData.getMessageId())
                                        .entities(
                                                manageMessageEntitiesShortMessage(
                                                        message.getMessageEntities(),
                                                        messageString,
                                                        SHORT_MESSAGE_SYMBOL_QUANTITY, suffix))
                                        .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(
                                                message.getId(),
                                                userData.getLanguageCode(),
                                                message.getType().equals(EMessageType.FORWARDED)))
                                        .file(message.getFile())
                                        .build(),
                                userData.getTelegramClient());
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleMessageChangeLanguage =
            userData -> {
                var user = userData.getUserService().getUser(userData.getUser().getId());

                sendEditMessage(userData.getChatId(),
                        String.format(userData.getMessageSourceService()
                                        .getMessage("messages.profile.language",
                                                user.getLanguageCode()),
                                AVAILABLE_LANGUAGES_MAP.get(user.getLanguageCode()).emoji()),
                        userData.getKeyboardFactoryService().getAvailableLanguage(user.getLanguageCode()),
                        null,
                        userData.getMessageId()
                );
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> howItWorksCallback =
            userData -> {
                sendEditMessage(userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                "messages.help.info",
                                userData.getLanguageCode()), userData.getUser().getFirstName()),
                        userData.getKeyboardFactoryService().getSingleBackKeyboard(userData.getLanguageCode()),
                        null,
                        userData.getMessageId()
                );
                return Boolean.TRUE;
            };

    private final Function<InputUserData, Boolean> handleEditConcreteMessage =
            userData -> {
                clearMessages(userData, List.of(WAIT_EDIT_TEXT, WAIT_EDIT_TEXT_CONCRETE));
                var editedMessage =
                        userData
                                .getMessageService()
                                .updateMessage(
                                        prepareEditedMessage(
                                                userData
                                                        .getMessageService()
                                                        .getMessageByTgExternalId(
                                                                userData.getMessageId().longValue(), userData.getUser().getId()),
                                                userData));

                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(editedMessage, false,
                        suffix, userData.getLanguageCode(), userData.getMessageSourceService());

                manageMsgType(editedMessage)
                        .sendMessage(
                                MessageDataRequest.builder()
                                        .chatId(userData.getChatId())
                                        .messageText(messageString)
                                        .entities(
                                                manageMessageEntitiesShortMessage(
                                                        editedMessage.getMessageEntities(),
                                                        messageString,
                                                        SHORT_MESSAGE_SYMBOL_QUANTITY, suffix))
                                        .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(
                                                editedMessage.getId(),
                                                userData.getLanguageCode(),
                                                editedMessage.getType().equals(EMessageType.FORWARDED)))
                                        .file(editedMessage.getFile())
                                        .build(),
                                userData.getTelegramClient());
                return Boolean.TRUE;
            };

    private Message sendMessage(long chatId, String text) {
        return sendMessage(chatId, text, null);
    }

    private Message sendMessage(long chatId, String text, ReplyKeyboard replyKeyboard) {
        return sendMessage(chatId, text, replyKeyboard, null);
    }

    private Message sendMessage(
            long chatId, String text, ReplyKeyboard replyKeyboard, List<MessageEntity> entities) {
        return sendMessage(chatId, text, replyKeyboard, entities, null);
    }

    private Message sendMessage(
            long chatId,
            String text,
            ReplyKeyboard replyKeyboard,
            List<MessageEntity> entities,
            Long replyMessageId) {
        try {
            return telegramClient.execute(
                    SendMessage.builder()
                            .chatId(chatId)
                            .text(text)
                            .parseMode(entities == null || entities.isEmpty() ? "markdown" : null)
                            .replyMarkup(replyKeyboard)
                            .entities(entities)
                            .replyToMessageId(null != replyMessageId ? replyMessageId.intValue() : null)
                            .build());
        } catch (TelegramApiException e) {
            log.error("Error: ", e);
            throw new RuntimeException(e);
        }
    }

    private void sendEditMessage(
            long chatId,
            String text,
            InlineKeyboardMarkup replyKeyboard,
            List<MessageEntity> entities,
            Integer messageId) {
        try {
            telegramClient.execute(
                    EditMessageText.builder()
                            .chatId(chatId)
                            .messageId(messageId)
                            .text(text)
                            .parseMode(entities == null || entities.isEmpty() ? "markdown" : null)
                            .replyMarkup(replyKeyboard)
                            .entities(entities)
                            .build());
        } catch (TelegramApiException e) {
            log.error("Error: ", e);
            throw new RuntimeException(e);
        }
    }

    private void deleteMessage(long chatId, int messageId) {
        try {
            telegramClient.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
        } catch (TelegramApiException e) {
            log.error("Error: ", e);
        }
    }

    private void deleteMessages(long chatId, Collection<Integer> messageIds) {
        try {
            telegramClient.execute(DeleteMessages.builder().chatId(chatId).messageIds(messageIds).build());
        } catch (TelegramApiException e) {
            log.error("Error: ", e);
        }
    }

    public static String parseMessage(EMessage message,
                                      boolean isFull,
                                      String valueSuffix,
                                      String languageCode,
                                      MessageSourceService messageSourceService) {
        return parseMessage(message, isFull, true, false, SHORT_MESSAGE_SYMBOL_QUANTITY, valueSuffix, languageCode, messageSourceService);
    }

    private static String parseMessage(
            EMessage message,
            boolean isFull,
            boolean isExecutionTime,
            boolean isTrimParagraph,
            int maxLength,
            String valueSuffix,
            String languageCode,
            MessageSourceService messageSourceService) {
        var result = new StringBuilder();

        if (null != message.getText() && !message.getText().isEmpty()) {
            var text = message.getText();

            if (isFull) {
                result.append(text);
            } else {
                if (isTrimParagraph) {
                    text = text.replaceAll("\n\n", "");
                }
                result.append(text, 0, Math.min(text.length(), maxLength));
                if (text.length() > maxLength) {
                    result.append(DOTS_STR);
                }
            }
        }

        if (isExecutionTime) {
            result
                    .append("\n\n")
                    .append(valueSuffix)
                    .append(formatDuration(LocalDateTime.now(UTC),
                            message.getNextExecutionDateTime(),
                            languageCode,
                            messageSourceService
                    ));
        }

        return result.toString();
    }

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

        return new MessageTuple(eMessage, getCategories(userData, eMessage.getType().equals(EMessageType.FORWARDED)));
    }

    private MessageTuple parseInput(InputUserData userData) {
        var input = userData.getMessageText();

        return new MessageTuple(
                EMessage.builder()
                        .text(input)
                        .messageEntities(
                                Optional.ofNullable(userData.getMessageEntities())
                                        .map(entities ->
                                                entities.stream()
                                                        .map(me -> {
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
        userData
                .getChatMessageStateService()
                .getMessages(userData.getUser().getId(), userData.getChatId(), awaitingCustomName)
                .forEach(msgId -> deleteMessage(userData.getChatId(), msgId));
        userData
                .getChatMessageStateService()
                .clearStateMessages(userData.getUser().getId(), userData.getChatId(), awaitingCustomName);
    }

    private void clearMessages(InputUserData userData, List<UserState> statuses) {
        statuses.forEach(s -> {
            var messages = userData
                    .getChatMessageStateService()
                    .getMessages(userData.getUser().getId(), userData.getChatId(), s);
            if (!messages.isEmpty()) {
                deleteMessages(
                        userData.getChatId(),
                        messages);
            }
            userData
                    .getChatMessageStateService()
                    .clearStateMessages(userData.getUser().getId(), userData.getChatId(), s);
        });

    }

    @NotNull
    private static Set<Category> getCategories(InputUserData userData, boolean isForwardedMessage) {
        return Optional.ofNullable(userData.getMessageEntities())
                .map(entities -> {
                    Set<Category> hashtags = entities.stream()
                            .filter(me -> me.getType().equals("hashtag"))
                            .map(MessageEntity::getText)
                            .distinct()
                            .map(category -> Category.builder().name(category).build())
                            .collect(Collectors.toSet());

                    return !hashtags.isEmpty()
                            ? hashtags
                            : manageDefaultCategory(isForwardedMessage);
                })
                .orElse(manageDefaultCategory(isForwardedMessage));
    }

    private static Set<Category> manageDefaultCategory(boolean isForwardedMessage) {
        return Set.of(
                Category.builder()
                        .name(isForwardedMessage ? FORWARDED : UNCATEGORIZED)
                        .build());
    }
}

package com.ebbinghaus.memory.app.bot;

import com.ebbinghaus.memory.app.domain.*;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.model.MessageType;
import com.ebbinghaus.memory.app.model.UserState;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.ebbinghaus.memory.app.model.UserState.*;
import static com.ebbinghaus.memory.app.utils.Constants.*;
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

    private static final Map<String, Consumer<InputUserData>> consumerCommandMap = new HashMap<>();
    private static final Map<String, Consumer<InputUserData>> consumerCallbackDataMap = new HashMap<>();
    private static final Map<UserState, Consumer<InputUserData>> consumerUserStateMap = new HashMap<>();

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

        consumerCommandMap.put(START, handleStartMessage);
        consumerCommandMap.put(ADD_NEW_INFO, handleButtonAddNewInfo);
        consumerCommandMap.put(INFO_LIST, handleButtonInfoList);
        consumerCommandMap.put(CATEGORY_LIST, handleButtonCategoryList);
        consumerCommandMap.put(PROFILE_LIST, handleProfileMainMenu);

        consumerCallbackDataMap.put(VIEW_MESSAGE_CALLBACK, handleMessageView);
        consumerCallbackDataMap.put(VIEW_SHORT_MESSAGE_CALLBACK, handleShortMessageView);
        consumerCallbackDataMap.put(EDIT_MESSAGE_CALLBACK, handleMessageEdit);
        consumerCallbackDataMap.put(DELETE_MESSAGE_CALLBACK, handleMessageDelete);
        consumerCallbackDataMap.put(BACK_MESSAGE_CALLBACK, handleMessageBack);
        consumerCallbackDataMap.put(NAVIGATION_DATA_LIST_CALLBACK, handleButtonInfoList);
        consumerCallbackDataMap.put(NAVIGATION_CATEGORY_LIST_CALLBACK, handleButtonCategoryList);
        consumerCallbackDataMap.put(EDIT_CONCRETE_MESSAGE_CALLBACK, handleEditConcreteMessage);
        consumerCallbackDataMap.put(DELETE_MESSAGE_YES_CALLBACK, handleMessageDeleteYes);
        consumerCallbackDataMap.put(DELETE_MESSAGE_NO_CALLBACK, handleMessageDeleteNo);
        consumerCallbackDataMap.put(VIEW_PROFILE_LANGUAGE_CALLBACK, handleMessageChangeLanguage);
        consumerCallbackDataMap.put(PROFILE_MAIN_MENU_CALLBACK, handleProfileMainMenuBack);
        consumerCallbackDataMap.put(CHANGE_PROFILE_LANGUAGE_CALLBACK, handleChangeLanguage);
        consumerCallbackDataMap.put(CONTACT_INFO_CALLBACK, handleContactInfo);

        consumerUserStateMap.put(WAIT_TEXT, handleInputText);
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
        // forward - message.getForwardDate - .getForwardOrigin -- private chat
        // forward - message.getForwardedFromChat - message.getForwardFromMSGId - .getForwardOrigin --
        // channel
        if (update.hasMessage()) {
            var msgType = manageMsgType(update.getMessage());

            var inputUserData =
                    InputUserData.builder()
                            .messageType(msgType)
                            .chatId(update.getMessage().getChatId())
                            .user(update.getMessage().getFrom())
                            .messageId(update.getMessage().getMessageId())
                            .file(msgType.getFile(update.getMessage()))
                            .messageEntities(msgType.getMsgEntities(update.getMessage()))
                            .messageText(msgType.getMsgText(update.getMessage()))
                            .languageCode(userService.findUser(update.getMessage().getFrom().getId())
                                    .map(EUser::getLanguageCode)
                                    .orElse(update.getMessage().getFrom().getLanguageCode())
                            )
                            .state(userService.getUserState(update.getMessage().getFrom().getId()))
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
                                                    ? consumerCommandMap.get(inputUserData.getMessageText().split(" ")[0])
                                                    : consumerCommandMap.get(extractSubstringForButton(inputUserData.getMessageText())))
                                    .ifPresentOrElse(consumer -> consumer.accept(inputUserData),
                                            () -> Optional.ofNullable(consumerUserStateMap.get(inputUserData.getState()))
                                                    .ifPresent(consumer -> consumer.accept(inputUserData))),
                            () -> Optional.ofNullable(consumerUserStateMap.get(inputUserData.getState()))
                                    .ifPresent(consumer -> consumer.accept(inputUserData)));
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

            consumerCallbackDataMap.get(callBackData.get(OPERATION)).accept(inputUserData);
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

            consumerCallbackDataMap.get(EDIT_CONCRETE_MESSAGE_CALLBACK).accept(inputUserData);
        }
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

    private final Consumer<InputUserData> handleStartMessage =
            userData -> {

                sendMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                "messages.greeting.start",
                                userData.getLanguageCode()), userData.getUser().getFirstName()),
                        userData.getKeyboardFactoryService().getMainMenuKeyboard(userData.getLanguageCode()));

                userData.getUserService().addUser(userData.getUser());
            };


    private final Consumer<InputUserData> handleProfileMainMenu =
            userData -> {
                deleteMessage(userData.getChatId(), userData.getMessageId());

                sendMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                "messages.profile",
                                userData.getLanguageCode()), userData.getUser().getFirstName()),
                        userData.getKeyboardFactoryService().getProfileKeyboard(userData.getLanguageCode()));
            };

    private final Consumer<InputUserData> handleProfileMainMenuBack =
            userData -> {
                sendEditMessage(
                        userData.getChatId(),
                        String.format(userData.getMessageSourceService().getMessage(
                                "messages.profile",
                                userData.getLanguageCode()), userData.getUser().getFirstName()),
                        userData.getKeyboardFactoryService().getProfileKeyboard(userData.getLanguageCode()),
                        null,
                        userData.getMessageId());
            };

    private final Consumer<InputUserData> handleChangeLanguage =
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
            };

    private final Consumer<InputUserData> handleContactInfo =
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
            };

    // refactor with db
    private final Consumer<InputUserData> handleInputText =
            userData -> {
                var message = userData.getMessageService().addMessage(parseInput(userData));
                var suffix = userData.getMessageSourceService().getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, false, suffix);
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
                                        .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(message.getId(), userData.getLanguageCode()))
                                        .file(message.getFile())
                                        .build(),
                                userData.getTelegramClient());

                userData.getSchedulerService().scheduleMessage(message, userData);
                userData.getUserService().setUserState(userData.getUser().getId(), MAIN_MENU);
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

    private final Consumer<InputUserData> handleButtonAddNewInfo =
            userData -> {
                cleanMessagesInStatus(userData, List.of(WAIT_EDIT_TEXT_CONCRETE));
                deleteMessage(userData.getChatId(), userData.getMessageId());

                var message = sendMessage(userData.getChatId(), userData.getMessageSourceService().getMessage("messages.input.waiting-data", userData.getLanguageCode()));
                userData
                        .getChatMessageStateService()
                        .addMessage(
                                userData.getUser().getId(),
                                userData.getChatId(),
                                WAIT_TEXT,
                                List.of(message.getMessageId()));

                userData.getUserService().setUserState(userData.getUser().getId(), WAIT_TEXT);
            };

    private final Consumer<InputUserData> handleButtonInfoList =
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
                            var msgString = parseMessage(m, false, false, true, SHORT_ELEMENT_LENGTH, suffix);
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
            };

    private final Consumer<InputUserData> handleButtonCategoryList =
            userData -> {
                clearMessages(userData, WAIT_EDIT_TEXT_CONCRETE);

                var page = getPage(userData);
                var size = getSize(userData, DEFAULT_CATEGORY_PAGE_SIZE);

                var categories =
                        userData
                                .getCategoryService()
                                .getCategories(
                                        userData.getUser().getId(), page, size, Sort.by(Sort.Order.desc("id")));

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

    private final Consumer<InputUserData> handleMessageView =
            userData -> {
                var message =
                        userData
                                .getMessageService()
                                .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);

                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, true, suffix);

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
                                        .replyKeyboard(userData.getKeyboardFactoryService().getViewKeyboard(message.getId(), userData.getLanguageCode()))
                                        .file(message.getFile())
                                        .build(),
                                userData.getTelegramClient());
            };

    private final Consumer<InputUserData> handleShortMessageView =
            userData -> {
                clearMessages(userData, SHORT_MESSAGE);

                var message =
                        userData
                                .getMessageService()
                                .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);

                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, false,
                        suffix);

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
                                                .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(message.getId(), userData.getLanguageCode()))
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
            };

    private final Consumer<InputUserData> handleMessageBack =
            userData -> {
                var message =
                        userData
                                .getMessageService()
                                .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);
                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, false,
                        suffix);

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
                                        .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(message.getId(), userData.getLanguageCode()))
                                        .file(message.getFile())
                                        .build(),
                                userData.getTelegramClient());
            };

    private final Consumer<InputUserData> handleMessageEdit =
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
            };

    private final Consumer<InputUserData> handleMessageDelete =
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
            };

    private final Consumer<InputUserData> handleMessageDeleteYes =
            userData -> {
                deleteMessage(userData.getChatId(), userData.getMessageId());

                userData
                        .getMessageService()
                        .deleteMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)));
            };

    private final Consumer<InputUserData> handleMessageDeleteNo =
            userData -> {
                var message =
                        userData
                                .getMessageService()
                                .getMessage(Long.valueOf(userData.getCallBackData().get(MESSAGE_ID)), true);

                var suffix = userData
                        .getMessageSourceService()
                        .getMessage("messages.suffix.execution-time", userData.getLanguageCode());
                var messageString = parseMessage(message, false, suffix
                );

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
                                        .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(message.getId(), userData.getLanguageCode()))
                                        .file(message.getFile())
                                        .build(),
                                userData.getTelegramClient());
            };

    private final Consumer<InputUserData> handleMessageChangeLanguage =
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
            };

    private final Consumer<InputUserData> handleEditConcreteMessage =
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
                        suffix);

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
                                        .replyKeyboard(userData.getKeyboardFactoryService().getMessageKeyboard(editedMessage.getId(), userData.getLanguageCode()))
                                        .file(editedMessage.getFile())
                                        .build(),
                                userData.getTelegramClient());
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

    public static String parseMessage(EMessage message, boolean isFull, String valueSuffix) {
        return parseMessage(message, isFull, true, false, SHORT_MESSAGE_SYMBOL_QUANTITY, valueSuffix);
    }

    private static String parseMessage(
            EMessage message,
            boolean isFull,
            boolean isExecutionTime,
            boolean isTrimParagraph,
            int maxLength,
            String valueSuffix) {
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
                    .append(formatDuration(LocalDateTime.now(UTC), message.getNextExecutionDateTime()));
        }

        return result.toString();
    }

    private EMessage prepareEditedMessage(EMessage eMessage, InputUserData userData) {
        eMessage
                .setText(userData.getMessageText())
                .setFile(userData.getFile())
                .setCategories(getCategories(userData))
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
        return eMessage;
    }

    private EMessage parseInput(InputUserData userData) {
        var input = userData.getMessageText();

        return EMessage.builder()
                .text(input)
                .categories(getCategories(userData))
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
                .build();
    }

    private void cleanMessagesInStatus(InputUserData userData, List<UserState> awaitingCustomNames) {
        awaitingCustomNames.forEach(
                awaitingCustomName ->
                        userData
                                .getChatMessageStateService()
                                .clearStateMessages(
                                        userData.getUser().getId(), userData.getChatId(), awaitingCustomName));
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
            userData
                    .getChatMessageStateService()
                    .getMessages(userData.getUser().getId(), userData.getChatId(), s)
                    .forEach(msgId -> deleteMessage(userData.getChatId(), msgId));
            userData
                    .getChatMessageStateService()
                    .clearStateMessages(userData.getUser().getId(), userData.getChatId(), s);
        });

    }

    @NotNull
    private static Set<Category> getCategories(InputUserData userData) {
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
                            : Set.of(
                            Category.builder()
                                    .name(UNCATEGORIZED)
                                    .build());
                })
                .orElse(Set.of(
                        Category.builder()
                                .name(UNCATEGORIZED)
                                .build()));
    }
}

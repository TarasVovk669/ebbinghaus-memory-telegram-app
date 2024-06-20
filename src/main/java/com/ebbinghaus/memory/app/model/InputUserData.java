package com.ebbinghaus.memory.app.model;

import com.ebbinghaus.memory.app.bot.KeyboardFactoryService;
import com.ebbinghaus.memory.app.domain.File;
import com.ebbinghaus.memory.app.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Map;

@Data
@Builder
@ToString
public class InputUserData {

    private File file;
    private User user;
    private Long chatId;
    private UserState state;
    private String ownerName;
    private Integer messageId;
    private String messageText;
    private String languageCode;
    private MessageType messageType;
    private boolean isForwardedMessage;
    private Map<String, String> callBackData;
    private List<MessageEntity> messageEntities;

    private UserService userService;
    private QuizService quizService;
    private ObjectMapper objectMapper;
    private MessageService messageService;
    private TelegramClient telegramClient;
    private CategoryService categoryService;
    private SchedulerService schedulerService;
    private MessageSourceService messageSourceService;
    private KeyboardFactoryService keyboardFactoryService;
    private ChatMessageStateService chatMessageStateService;






}

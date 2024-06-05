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

  private Long chatId;

  private String messageText;

  private User user;

  private UserState state;

  private String languageCode;

  private Integer messageId;

  private String ownerName;

  private boolean isForwardedMessage;

  private List<MessageEntity> messageEntities;

  private MessageType messageType;

  private File file;

  private Map<String, String> callBackData;

  private TelegramClient telegramClient;

  private UserService userService;

  private MessageService messageService;

  private CategoryService categoryService;

  private ChatMessageStateService chatMessageStateService;

  private SchedulerService schedulerService;

  private ObjectMapper objectMapper;

  private MessageSourceService messageSourceService;

  private KeyboardFactoryService keyboardFactoryService;

}

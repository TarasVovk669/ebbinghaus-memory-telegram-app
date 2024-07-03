package com.ebbinghaus.memory.app.bot;

import static com.ebbinghaus.memory.app.model.UserState.*;
import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.MessageUtils.*;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.*;

import com.ebbinghaus.memory.app.domain.*;
import com.ebbinghaus.memory.app.model.*;
import com.ebbinghaus.memory.app.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public class MemoryBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

  private final String token;
  private final String ownerName;
  private final UserService userService;
  private final ObjectMapper objectMapper;
  private final TelegramBotService telegramBotService;

  public MemoryBot(
      @Value("${bot.token}") String token,
      @Value("${bot.owner}") String ownerName,
      UserService userService,
      ObjectMapper objectMapper,
      TelegramBotService telegramBotService) {
    this.token = token;
    this.ownerName = ownerName;
    this.userService = userService;
    this.objectMapper = objectMapper;
    this.telegramBotService = telegramBotService;
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
      var isForwardedMessage =
          null != update.getMessage().getForwardOrigin()
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
              .languageCode(
                  userService
                      .findUser(update.getMessage().getFrom().getId())
                      .map(EUser::getLanguageCode)
                      .orElse(update.getMessage().getFrom().getLanguageCode()))
              .state(
                  isForwardedMessage
                      ? WAIT_FORWARDED_MESSAGE
                      : userService.getUserState(update.getMessage().getFrom().getId()))
              .build();

      telegramBotService.processTextInputCallback(inputUserData);
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
              .languageCode(
                  userService
                      .getUser(update.getCallbackQuery().getFrom().getId())
                      .getLanguageCode())
              .state(userService.getUserState(update.getCallbackQuery().getFrom().getId()))
              .messageId(update.getCallbackQuery().getMessage().getMessageId())
              .build();

      telegramBotService.processButtonMessageCallback(callBackData.get(OPERATION), inputUserData);
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
              .build();

      telegramBotService.processEditMessageCallback(EDIT_CONCRETE_MESSAGE_CALLBACK, inputUserData);
    }
  }
}

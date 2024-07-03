package com.ebbinghaus.memory.app.service.impl;

import static com.ebbinghaus.memory.app.utils.Constants.MARKDOWN;
import static com.ebbinghaus.memory.app.utils.Constants.MESSAGE_CAN_T_BE_DELETED_FOR_EVERYONE;
import static com.ebbinghaus.memory.app.utils.MessageUtils.manageMsgType;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTryTgCall;

import com.ebbinghaus.memory.app.bot.MemoryBot;
import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.model.MessageType;
import com.ebbinghaus.memory.app.service.MessageSourceService;
import com.ebbinghaus.memory.app.service.TelegramClientService;
import java.io.File;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
@RequiredArgsConstructor
public class TelegramClientServiceImpl implements TelegramClientService {

  private static final Logger log = LoggerFactory.getLogger(MemoryBot.class);

  private final TelegramClient telegramClient;
  private final MessageSourceService messageSourceService;

  @Override
  public Message sendMessage(MessageType messageType, MessageDataRequest request) {
    return messageType.sendMessage(request, telegramClient);
  }

  @Override
  public void sendEditMessage(MessageType messageType, MessageDataRequest request) {
    messageType.editMessage(request, telegramClient);
  }

  @Override
  public void sendEditMessage(EditMessageText editMessage) throws TelegramApiException {
    telegramClient.execute(editMessage);
  }

  @Override
  public Message sendMessage(Long chatId, String text) {
    return sendMessage(chatId, text, null);
  }

  @Override
  public Message sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
    return sendMessage(chatId, text, replyKeyboard, null);
  }

  @Override
  public Message sendMessage(
      Long chatId, String text, ReplyKeyboard replyKeyboard, List<MessageEntity> entities) {
    return sendMessage(chatId, text, replyKeyboard, entities, null);
  }

  @Override
  public Message sendMessage(
      Long chatId,
      String text,
      ReplyKeyboard replyKeyboard,
      List<MessageEntity> entities,
      Long replyMessageId) {
    try {
      return telegramClient.execute(
          SendMessage.builder()
              .chatId(chatId)
              .text(text)
              .parseMode(entities == null || entities.isEmpty() ? MARKDOWN : null)
              .replyMarkup(replyKeyboard)
              .entities(entities)
              .replyToMessageId(null != replyMessageId ? replyMessageId.intValue() : null)
              .build());
    } catch (TelegramApiException e) {
      log.error("Error: ", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendEditMessage(
      Long chatId,
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
              .parseMode(entities == null || entities.isEmpty() ? MARKDOWN : null)
              .replyMarkup(replyKeyboard)
              .entities(entities)
              .build());
    } catch (TelegramApiException e) {
      log.error("Error: ", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Message sendPhotoMessage(
      Long chatId, String text, ReplyKeyboard replyKeyboard, String url, String fileId) {
    return doTryTgCall(
        () ->
            telegramClient.execute(
                SendPhoto.builder()
                    .chatId(chatId)
                    .caption(text)
                    .parseMode(MARKDOWN)
                    .replyMarkup(replyKeyboard)
                    .photo(null != fileId ? new InputFile(fileId) : new InputFile(new File(url)))
                    .build()));
  }

  @Override
  public void deleteMessage(Long chatId, int messageId) {
    try {
      telegramClient.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
    } catch (TelegramApiException e) {
      log.warn(
          "Error to delete messages with chat_id:{} and message_id: {} and error_message: {}",
          chatId,
          messageId,
          e.getMessage());
    }
  }

  @Override
  public void deleteMessage(Long chatId, int messageId, String languageCode, EMessage message) {
    try {
      telegramClient.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
    } catch (TelegramApiException e) {
      log.warn(
          "Error to delete message with chat_id:{} and message_id: {} and error_message: {}",
          chatId,
          messageId,
          e.getMessage());

      if (e.getMessage().contains(MESSAGE_CAN_T_BE_DELETED_FOR_EVERYONE)) {
        log.warn("Edit existing message with id: {}", messageId);

        manageMsgType(message)
            .editMessage(
                MessageDataRequest.builder()
                    .chatId(chatId)
                    .messageText(
                        messageSourceService.getMessage(
                            "messages.error.tg_msg_not_allow_delete", languageCode))
                    .messageId(messageId)
                    .file(message.getFile())
                    .build(),
                telegramClient);
      }
    }
  }

  @Override
  public void deleteMessages(Long chatId, Collection<Integer> messageIds) {
    try {
      telegramClient.execute(
          DeleteMessages.builder().chatId(chatId).messageIds(messageIds).build());
    } catch (TelegramApiException e) {
      log.warn(
          "Error to delete messages with chat_id:{} and message_ids: {} and error_message: {}",
          chatId,
          messageIds,
          e.getMessage());
    }
  }
}

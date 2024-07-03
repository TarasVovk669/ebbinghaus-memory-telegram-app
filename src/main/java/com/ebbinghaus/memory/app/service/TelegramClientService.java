package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.model.MessageType;
import java.util.Collection;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface TelegramClientService {

  Message sendMessage(Long chatId, String text);

  Message sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard);

  Message sendMessage(
      Long chatId, String text, ReplyKeyboard replyKeyboard, List<MessageEntity> entities);

  Message sendMessage(
      Long chatId,
      String text,
      ReplyKeyboard replyKeyboard,
      List<MessageEntity> entities,
      Long replyMessageId);

  void sendEditMessage(
      Long chatId,
      String text,
      InlineKeyboardMarkup replyKeyboard,
      List<MessageEntity> entities,
      Integer messageId);

  Message sendPhotoMessage(
      Long chatId, String text, ReplyKeyboard replyKeyboard, String url, String fileId);

  void deleteMessage(Long chatId, int messageId);

  void deleteMessage(Long chatId, int messageId, String languageCode, EMessage message);

  void deleteMessages(Long chatId, Collection<Integer> messageIds);

  Message sendMessage(MessageType messageType, MessageDataRequest build);

  void sendEditMessage(MessageType messageType, MessageDataRequest request);

  void sendEditMessage(EditMessageText editMessage) throws TelegramApiException;
}

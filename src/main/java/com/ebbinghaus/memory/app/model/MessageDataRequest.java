package com.ebbinghaus.memory.app.model;

import com.ebbinghaus.memory.app.domain.File;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

@Data
@Builder
@ToString
public class MessageDataRequest {

  private Long chatId;

  private String messageText;

  private User user;

  private Integer messageId;

  private List<MessageEntity> entities;

  private MessageType messageType;

  private File file;

  private InlineKeyboardMarkup replyKeyboard;
  private Long replyMessageId;
}

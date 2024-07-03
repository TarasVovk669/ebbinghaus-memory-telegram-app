package com.ebbinghaus.memory.app.utils;

import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.DateUtils.formatDuration;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static java.time.ZoneOffset.UTC;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.domain.EMessageEntity;
import com.ebbinghaus.memory.app.domain.FileType;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.MessageType;
import com.ebbinghaus.memory.app.service.MessageSourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public class MessageUtils {

  public static List<MessageEntity> manageMessageEntitiesLongMessage(
      Collection<String> messageEntities,
      String messageString,
      boolean addSuffix,
      String suffixValue,
      ObjectMapper objectMapper) {
    var entities =
        new ArrayList<>(
            messageEntities.stream()
                .map(me -> doTry(() -> objectMapper.readValue(me, MessageEntity.class)))
                .toList());

    if (addSuffix) {
      entities.add(
          MessageEntity.builder()
              .type(BOLD_STYLE)
              .offset(messageString.lastIndexOf(suffixValue))
              .length(suffixValue.length())
              .build());
    }

    return entities;
  }

  public static List<MessageEntity> manageMessageEntitiesShortMessage(
      Collection<EMessageEntity> messageEntities,
      String messageString,
      Integer maxLength,
      String suffix,
      ObjectMapper objectMapper) {
    var entities = new ArrayList<>(getMessageEntities(messageEntities, maxLength, objectMapper));
    entities.add(
        MessageEntity.builder()
            .type(BOLD_STYLE)
            .offset(messageString.lastIndexOf(suffix))
            .length(suffix.length())
            .build());
    return entities;
  }

  public static List<MessageEntity> getMessageEntities(
      Collection<EMessageEntity> messageEntities, Integer maxLength, ObjectMapper objectMapper) {
    return Optional.ofNullable(messageEntities)
        .map(
            mes ->
                mes.stream()
                    .map(
                        me ->
                            doTry(() -> objectMapper.readValue(me.getValue(), MessageEntity.class)))
                    .filter(me -> me.getOffset() < maxLength)
                    .peek(
                        me -> {
                          if (me.getOffset() + me.getLength() > maxLength) {
                            me.setLength(maxLength - me.getOffset());
                          }
                        })
                    .toList())
        .orElse(Collections.emptyList());
  }

  public static MessageType manageMsgType(Message message) {
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

  public static String parseMessage(
      EMessage message,
      boolean isFull,
      String valueSuffix,
      String languageCode,
      MessageSourceService messageSourceService) {
    return parseMessage(
        message,
        isFull,
        true,
        false,
        SHORT_MESSAGE_SYMBOL_QUANTITY,
        valueSuffix,
        languageCode,
        messageSourceService);
  }

  public static String parseMessage(
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
          text = text.replaceAll("\n\n", "  ");
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
          .append(
              formatDuration(
                  LocalDateTime.now(UTC),
                  message.getNextExecutionDateTime(),
                  languageCode,
                  messageSourceService));
    }

    return result.toString();
  }

  public static Set<Category> getCategories(InputUserData userData, boolean isForwardedMessage) {
    return Optional.ofNullable(userData.getMessageEntities())
        .map(
            entities -> {
              Set<Category> hashtags =
                  entities.stream()
                      .filter(me -> me.getType().equals("hashtag"))
                      .map(MessageEntity::getText)
                      .distinct()
                      .map(category -> Category.builder().name(category).build())
                      .collect(Collectors.toSet());

              return !hashtags.isEmpty() ? hashtags : manageDefaultCategory(isForwardedMessage);
            })
        .orElse(manageDefaultCategory(isForwardedMessage));
  }

  public static Long getCategoryId(InputUserData userData) {
    return Optional.ofNullable(userData.getCallBackData())
        .map(cd -> cd.get(CATEGORY_ID))
        .map(Long::valueOf)
        .orElse(null);
  }

  public static int getSize(InputUserData userData, Integer defaultPageSize) {
    return Optional.ofNullable(userData.getCallBackData())
        .map(cd -> cd.get(SIZE))
        .map(Integer::valueOf)
        .orElse(defaultPageSize);
  }

  public static int getPage(InputUserData userData) {
    return Optional.ofNullable(userData.getCallBackData())
        .map(cd -> cd.get(PAGE))
        .map(Integer::valueOf)
        .orElse(0);
  }

  private static Set<Category> manageDefaultCategory(boolean isForwardedMessage) {
    return Set.of(Category.builder().name(isForwardedMessage ? FORWARDED : UNCATEGORIZED).build());
  }
}

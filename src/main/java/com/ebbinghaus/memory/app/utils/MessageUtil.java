package com.ebbinghaus.memory.app.utils;

import com.ebbinghaus.memory.app.domain.EMessageEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import java.util.*;

import static com.ebbinghaus.memory.app.utils.Constants.BOLD_STYLE;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;

public class MessageUtil {

    public static List<MessageEntity> manageMessageEntitiesLongMessage(
            Collection<String> messageEntities, String messageString, boolean addSuffix, String suffixValue, ObjectMapper objectMapper) {
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
            Collection<EMessageEntity> messageEntities, String messageString, Integer maxLength, String suffix, ObjectMapper objectMapper) {
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
                .map(mes ->
                        mes.stream()
                                .map(me -> doTry(() -> objectMapper.readValue(me.getValue(), MessageEntity.class)))
                                .filter(me -> me.getOffset() < maxLength)
                                .peek(me -> {
                                    if (me.getOffset() + me.getLength() > maxLength) {
                                        me.setLength(maxLength - me.getOffset());
                                    }
                                })
                                .toList())
                .orElse(Collections.emptyList());
    }
}

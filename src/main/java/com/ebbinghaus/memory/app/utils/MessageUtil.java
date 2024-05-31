package com.ebbinghaus.memory.app.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.ebbinghaus.memory.app.utils.Constants.BOLD_STYLE;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;

public class MessageUtil {

    public static List<MessageEntity> manageMessageEntitiesLongMessage(
            Collection<String> messageEntities, String messageString, boolean addSuffix, String suffixValue, ObjectMapper objectMapper) {
        var entities =
                new ArrayList<>(
                        messageEntities.stream()
                                .map(me -> doTry(() -> objectMapper.readValue(me, MessageEntity.class))
                                )
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
}

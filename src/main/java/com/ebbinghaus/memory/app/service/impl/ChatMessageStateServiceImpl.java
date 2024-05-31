package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.model.UserState;
import com.ebbinghaus.memory.app.service.ChatMessageStateService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMessageStateServiceImpl implements ChatMessageStateService {

    // todo: change for db
    private static final Map<String, Map<UserState, Set<Integer>>> CHAT_DATA_MAP =
            new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(ChatMessageStateServiceImpl.class);
    public static final HashSet<Integer> HASH_SET = new HashSet<>();

    @Override
    public void addMessage(
            Long userId, Long chatId, UserState state, Collection<Integer> messageIds) {
        log.info(
                "Add message with id: {}, chat_id: {}, user_id: {} and state: {}",
                messageIds,
                chatId,
                userId,
                state);

        String key = manageKey(userId, chatId);

        CHAT_DATA_MAP.compute(
                key,
                (k, stateMap) -> {
                    if (null == stateMap) {
                        stateMap = new HashMap<>();
                    }

                    stateMap.compute(
                            state,
                            (s, messageList) -> {
                                if (messageList == null) {
                                    messageList = new HashSet<>();
                                }
                                messageList.addAll(messageIds);
                                return messageList;
                            });
                    return stateMap;
                });
    }

    @Override
    public Set<Integer> getMessages(Long userId, Long chatId, UserState state) {
        log.info("Get messages - chat_id: {}, user_id: {} and state: {}", chatId, userId, state);

        return Optional.ofNullable(CHAT_DATA_MAP.get(manageKey(userId, chatId)))
                .map(map -> map.get(state))
                .orElse(HASH_SET);
    }

    @Override
    public void clearStateMessages(Long userId, Long chatId, UserState state) {
        log.info("clear messages - chat_id: {}, user_id: {} and state: {}", chatId, userId, state);

        var userStateListMap = CHAT_DATA_MAP.get(manageKey(userId, chatId));
        if (userStateListMap != null) {
            userStateListMap.remove(state);
        }
    }

    private static String manageKey(@NotNull Long userId, @NotNull Long chatId) {
        return chatId.toString().concat(userId.toString());
    }
}

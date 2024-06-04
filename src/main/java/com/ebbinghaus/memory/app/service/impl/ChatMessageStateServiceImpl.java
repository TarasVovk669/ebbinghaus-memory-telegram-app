package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.EMessageState;
import com.ebbinghaus.memory.app.domain.embedded.EMessageStateId;
import com.ebbinghaus.memory.app.model.UserState;
import com.ebbinghaus.memory.app.repository.MessageStateRepository;
import com.ebbinghaus.memory.app.service.ChatMessageStateService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatMessageStateServiceImpl implements ChatMessageStateService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageStateServiceImpl.class);
    public static final HashSet<Integer> HASH_SET = new HashSet<>();

    private final MessageStateRepository messageStateRepository;

    @Override
    public void addMessage(
            Long userId, Long chatId, UserState state, Collection<Integer> messageIds) {
        log.info(
                "Add message with id: {}, chat_id: {}, user_id: {} and state: {}",
                messageIds,
                chatId,
                userId,
                state);

        var id = EMessageStateId.builder()
                .chatId(chatId)
                .userId(userId)
                .state(state)
                .build();

        messageStateRepository.findById(id)
                .ifPresentOrElse(
                        ms -> {
                            ms.getMessageIds().addAll(messageIds);
                            messageStateRepository.save(ms);
                        },
                        () -> messageStateRepository.save(EMessageState.builder()
                                .id(id)
                                .messageIds(new HashSet<>(messageIds))
                                .build()));
    }

    @Override
    public Set<Integer> getMessages(Long userId, Long chatId, UserState state) {
        log.info("Get messages - chat_id: {}, user_id: {} and state: {}", chatId, userId, state);

        return messageStateRepository.findById(EMessageStateId.builder()
                .chatId(chatId)
                .userId(userId)
                .state(state)
                .build()).map(EMessageState::getMessageIds).orElse(HASH_SET);
    }

    @Override
    public void clearStateMessages(Long userId, Long chatId, UserState state) {
        log.info("clear messages - chat_id: {}, user_id: {} and state: {}", chatId, userId, state);

        messageStateRepository.deleteById(EMessageStateId.builder()
                .chatId(chatId)
                .userId(userId)
                .state(state)
                .build());
    }
}

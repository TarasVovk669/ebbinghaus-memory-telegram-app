package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.domain.EMessageEntity;
import com.ebbinghaus.memory.app.repository.MessageEntityRepository;
import com.ebbinghaus.memory.app.repository.MessageRepository;
import com.ebbinghaus.memory.app.service.CategoryService;
import com.ebbinghaus.memory.app.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.ebbinghaus.memory.app.utils.DateUtils.calculateNextExecutionTime;

@Service
@AllArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageRepository messageRepository;

    private final MessageEntityRepository messageEntityRepository;

    private final CategoryService categoryService;

    @Override
    @Transactional
    public EMessage addMessage(EMessage message) {
        log.info("Add message: {}", message);

        Map<String, Category> existingCategories =
                categoryService.existingCategories(message.getCategories(), message.getOwnerId());

        message.setCategories(
                Optional.ofNullable(message.getCategories())
                        .map(categories ->
                                categories.stream()
                                        .map(c -> existingCategories.getOrDefault(
                                                c.getName(),
                                                Category.builder()
                                                        .name(c.getName())
                                                        .ownerId(message.getOwnerId())
                                                        .build()))
                                        .collect(Collectors.toSet()))
                        .orElse(null));

        return messageRepository.save(message);
    }

    @Override
    @Transactional
    public EMessage updateMessage(EMessage message) {
        log.info("Update message: {}", message);

        Map<String, Category> existingCategories =
                categoryService.existingCategories(message.getCategories(), message.getOwnerId());

        message.setCategories(
                Optional.ofNullable(message.getCategories())
                        .map(categories ->
                                categories.stream()
                                        .map(c -> existingCategories.getOrDefault(
                                                c.getName(),
                                                Category.builder()
                                                        .name(c.getName())
                                                        .ownerId(message.getOwnerId())
                                                        .build()))
                                        .collect(Collectors.toSet()))
                        .orElse(null));

        message.setCategories(null);
        return messageRepository.save(message);
    }

    @Override
    public Page<EMessage> getMessages(Long userId, Long categoryId, int page, int size, Sort sort) {
        log.info(
                "Get messages for user with id: {},category_id: {}, page:{}, size:{}, sort:{}",
                userId,
                categoryId,
                page,
                size,
                sort);

        Page<EMessage> messages = Optional.ofNullable(categoryId)
                .map(cId -> messageRepository.getAllByOwnerIdAndCategories(
                        userId, cId, PageRequest.of(page, size, sort)))
                .orElse(messageRepository.getAllByOwnerId(userId, PageRequest.of(page, size, sort)));

        Map<Long, List<EMessageEntity>> messageEntitiesMap = getMessageEntitiesMap(messages);

        messages.forEach(
                m -> m.setMessageEntities(messageEntitiesMap.containsKey(m.getId())
                        ? new HashSet<>(messageEntitiesMap.get(m.getId())) : new HashSet<>())
        );

        return messages;
    }

    @Override
    public EMessage getMessage(Long id, boolean fetch) {
        log.info("Get message with id: {} and fetch: {}", id, fetch);

        return fetch
                ? messageRepository
                .getEMessageById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"))
                : messageRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
    }

    @Override
    @Transactional
    public EMessage getUpdatedMessage(Long id, boolean fetch) {
        log.info("Get updated_message with id: {} and fetch: {}", id, fetch);

        var message = fetch
                ? messageRepository
                .getEMessageById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found")) :
                messageRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        message.setExecutionStep(message.getExecutionStep() + 1);
        message.setNextExecutionDateTime(calculateNextExecutionTime(message));

        return message;
    }

    @Override
    public EMessage getMessageByTgExternalId(Long externalId, Long userId) {
        log.info("Get message with external_id: {} and user_id: {}", externalId, userId);

        return messageRepository
                .getEMessageByMessageIdAndOwnerId(externalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
    }

    @Override
    public void deleteMessage(Long id) {
        log.info("Delete messages with id: {}", id);
        messageRepository.deleteById(id);
    }

    @NotNull
    private Map<Long, List<EMessageEntity>> getMessageEntitiesMap(Page<EMessage> messages) {
        return messageEntityRepository.getAllByIdsIn(messages.stream()
                        .map(EMessage::getId)
                        .toList())
                .stream()
                .collect(Collectors.groupingBy(EMessageEntity::getMessageId));
    }
}

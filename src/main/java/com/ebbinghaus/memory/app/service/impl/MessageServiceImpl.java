package com.ebbinghaus.memory.app.service.impl;

import static com.ebbinghaus.memory.app.utils.DateUtils.calculateNextExecutionTime;
import static java.time.ZoneOffset.UTC;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.domain.EMessageCategory;
import com.ebbinghaus.memory.app.domain.EMessageEntity;
import com.ebbinghaus.memory.app.model.MessageTuple;
import com.ebbinghaus.memory.app.model.proj.CategoryMessageCountProj;
import com.ebbinghaus.memory.app.model.proj.DataMessageCategoryProj;
import com.ebbinghaus.memory.app.repository.MessageCategoryRepository;
import com.ebbinghaus.memory.app.repository.MessageEntityRepository;
import com.ebbinghaus.memory.app.repository.MessageRepository;
import com.ebbinghaus.memory.app.service.CategoryService;
import com.ebbinghaus.memory.app.service.MessageService;
import com.ebbinghaus.memory.app.service.UtilityService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MessageServiceImpl implements MessageService {

  private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

  private final CategoryService categoryService;
  private final UtilityService utilityService;
  private final MessageRepository messageRepository;
  private final MessageEntityRepository messageEntityRepository;
  private final MessageCategoryRepository messageCategoryRepository;

  @Override
  @Transactional
  @Caching(
      evict = {@CacheEvict(value = "get_user_profile_stat", key = "#messageTuple.message.ownerId")})
  public EMessage addMessage(MessageTuple messageTuple) {
    log.info("Add message: {}", messageTuple);
    var message = messageTuple.message();
    var existingCategories =
        categoryService.existingCategories(
            messageTuple.categories().stream().map(Category::getName).collect(Collectors.toSet()),
            message.getOwnerId());
    var result = messageRepository.save(message);

    messageCategoryRepository.saveAll(
        getMessageCategories(messageTuple, existingCategories, message));
    return result;
  }

  @Override
  @Transactional
  public EMessage updateMessage(MessageTuple messageTuple) {
    log.info("Update message: {}", messageTuple);
    var message = messageTuple.message();
    var existingCategories =
        categoryService.existingCategories(
            messageTuple.categories().stream().map(Category::getName).collect(Collectors.toSet()),
            message.getOwnerId());
    var updatedMessageCategories = getMessageCategories(messageTuple, existingCategories, message);

    manageUserCategories(message, updatedMessageCategories);

    messageCategoryRepository.saveAll(updatedMessageCategories);
    return messageRepository.save(message.setMessageCategories(updatedMessageCategories));
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

    Page<EMessage> messages =
        null != categoryId
            ? messageRepository.getAllByOwnerIdAndCategories(
                userId, categoryId, PageRequest.of(page, size, sort))
            : messageRepository.getAllByOwnerId(userId, PageRequest.of(page, size, sort));

    Map<Long, List<EMessageEntity>> messageEntitiesMap = getMessageEntitiesMap(messages);
    messages.forEach(
        m ->
            m.setMessageEntitiesDirectly(
                messageEntitiesMap.containsKey(m.getId())
                    ? new HashSet<>(messageEntitiesMap.get(m.getId()))
                    : new HashSet<>()));

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
  public Optional<EMessage> getMessageOptional(Long id, boolean fetch) {
    log.info("Get message with id: {} and fetch: {}", id, fetch);

    return fetch ? messageRepository.getEMessageById(id) : messageRepository.findById(id);
  }

  @Override
  @Transactional
  public EMessage getUpdatedMessage(Long id, boolean fetch) {
    log.info("Get updated_message with id: {} and fetch: {}", id, fetch);

    var message =
        fetch
            ? messageRepository
                .getEMessageById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"))
            : messageRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

    message.setExecutionStep(message.getExecutionStep() + 1);
    message.setNextExecutionDateTime(calculateNextExecutionTime(message));

    return message;
  }

  @Override
  @Transactional
  public EMessage getUpdatedMessage(Long id, Integer step, LocalDateTime executionTime) {
    log.info(
        "Get updated_message with id: {}, step: {}, executionTime: {}", id, step, executionTime);

    var message =
        messageRepository
            .getEMessageById(id)
            .orElseThrow(() -> new EntityNotFoundException("Message not found"));

    message.setExecutionStep(step);
    message.setNextExecutionDateTime(executionTime);

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
  @Transactional
  public void deleteMessage(Long id, Long chatId) {
    log.info("Delete messages with id: {}", id);

    messageRepository
        .getEMessageById(id)
        .ifPresent(
            message -> {
              manageMessageCategoryWithCategory(message, message.getMessageCategories());
              messageRepository.deleteById(id);
              utilityService.removeSchedulerTrigger(id, chatId);
            });
  }

  @Override
  @Cacheable(value = "get_user_profile_stat", key = "#ownerId")
  public DataMessageCategoryProj getMessageAndCategoryCount(Long ownerId) {
    log.info("Get message and category count for user_id: {}", ownerId);
    return messageRepository.getMessageAndCategoryCount(ownerId);
  }

  @Override
  public EMessage restartMessageAndSchedule(Long messageId, Long chatId) {
    log.info("Restart learning message with id: {}", messageId);

    var updatedMessage =
        messageRepository.save(
            messageRepository
                .findById(messageId)
                .map(
                    message ->
                        message
                            .setExecutionStep(1)
                            .setNextExecutionDateTime(
                                calculateNextExecutionTime(LocalDateTime.now(UTC))))
                .orElseThrow(() -> new EntityNotFoundException("Message not found")));

    utilityService.rescheduleJob(updatedMessage, chatId);
    return updatedMessage;
  }

  @NotNull
  private Map<Long, List<EMessageEntity>> getMessageEntitiesMap(Page<EMessage> messages) {
    return messageEntityRepository
        .getAllByIdsIn(messages.stream().map(EMessage::getId).toList())
        .stream()
        .collect(Collectors.groupingBy(EMessageEntity::getMessageId));
  }

  @NotNull
  private Set<EMessageCategory> getMessageCategories(
      MessageTuple messageTuple, Map<String, Category> existingCategories, EMessage message) {
    return Optional.ofNullable(messageTuple.categories())
        .map(
            categories ->
                categories.stream()
                    .map(
                        category ->
                            existingCategories.containsKey(category.getName())
                                ? existingCategories.get(category.getName())
                                : categoryService.save(
                                    Category.builder()
                                        .name(category.getName())
                                        .ownerId(message.getOwnerId())
                                        .build()))
                    .collect(Collectors.toSet()))
        .orElse(new HashSet<>())
        .stream()
        .map(c -> new EMessageCategory(message, c))
        .collect(Collectors.toSet());
  }

  private void manageUserCategories(
      EMessage message, Collection<EMessageCategory> updatedMessageCategories) {
    List<EMessageCategory> difference = new ArrayList<>(message.getMessageCategories());
    difference.removeAll(updatedMessageCategories);

    manageMessageCategoryWithCategory(message, difference);
  }

  private void manageMessageCategoryWithCategory(
      EMessage message, Collection<EMessageCategory> difference) {
    messageCategoryRepository.deleteAll(difference);
    List<Long> result =
        categoryService
            .findCategoryMessageCounts(
                message.getOwnerId(),
                difference.stream()
                    .map(mc -> mc.getId().getCategoryId())
                    .collect(Collectors.toList()))
            .stream()
            .filter(c -> c.getMsgQuantity().equals(0L))
            .map(CategoryMessageCountProj::getId)
            .toList();

    if (!result.isEmpty()) {
      categoryService.deleteById(result, message.getOwnerId());
    }
  }
}

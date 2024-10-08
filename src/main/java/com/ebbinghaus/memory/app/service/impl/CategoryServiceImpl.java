package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.model.CategoryMessageCount;
import com.ebbinghaus.memory.app.model.proj.CategoryMessageCountProj;
import com.ebbinghaus.memory.app.repository.CategoryRepository;
import com.ebbinghaus.memory.app.service.CategoryService;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

  private final CategoryRepository categoryRepository;

  @Override
  public Page<CategoryMessageCount> getCategories(Long userId, int page, int size, Sort sort) {
    log.info(
        "Get categories for user with id: {}, page:{}, size:{}, sort:{}", userId, page, size, sort);

    return categoryRepository.findCategoryMessageCounts(userId, PageRequest.of(page, size, sort));
  }

  @Override
  public Map<String, Category> existingCategories(Set<String> categoriesName, Long ownerId) {
    return Optional.ofNullable(categoriesName)
        .or(() -> Optional.of(Set.of("#uncategorized")))
        .map(
            names -> {
              var categories = categoryRepository.findAllByOwnerIdAndNameIn(ownerId, names);

              return categories.stream()
                  .map(
                      category ->
                          Category.builder()
                              .id(category.id())
                              .name(category.name())
                              .ownerId(category.ownerId())
                              .createdDateTime(category.createdDateTime())
                              .updatedDateTime(category.updatedDateTime())
                              .build())
                  .collect(Collectors.toMap(Category::getName, category -> category));
            })
        .orElse(new HashMap<>());
  }

  @Override
  @Caching(evict = {@CacheEvict(value = "get_user_profile_stat", key = "#category.ownerId")})
  public Category save(Category category) {
    log.info("Save category: {}", category);

    return categoryRepository.save(category);
  }

  @Override
  public List<CategoryMessageCountProj> findCategoryMessageCounts(Long ownerId, List<Long> ids) {
    log.info("Find categories by owner id: {} and ids: {}", ownerId, ids);
    return categoryRepository.findCategoryMessageCounts(ownerId, ids);
  }

  @Override
  @Caching(evict = {@CacheEvict(value = "get_user_profile_stat", key = "#ownerId")})
  public void deleteById(Collection<Long> ids, Long ownerId) {
    log.info("Delete categories by ids: {} and ownerId: {}", ids, ownerId);
    categoryRepository.deleteAllById(ids);
  }
}

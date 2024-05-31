package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.model.CategoryMessageCount;
import com.ebbinghaus.memory.app.repository.CategoryRepository;
import com.ebbinghaus.memory.app.service.CategoryService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
    public Map<String, Category> existingCategories(Set<Category> categoriesList, Long ownerId) {
        return Optional.ofNullable(categoriesList)
                .or(() -> Optional.of(
                        Set.of(
                                Category.builder()
                                        .name("#uncategorized")
                                        .build())))
                .map(
                        categories ->
                                categoryRepository
                                        .findAllByOwnerIdAndNameIn(
                                                ownerId,
                                                categories.stream().map(Category::getName).toList())
                                        .stream()
                                        .map(category -> Category.builder()
                                                .id(category.id())
                                                .name(category.name())
                                                .ownerId(category.ownerId())
                                                .createdDateTime(category.createdDateTime())
                                                .updatedDateTime(category.updatedDateTime())
                                                .build())
                                        .collect(Collectors.toMap(Category::getName, category -> category)))
                .orElse(new HashMap<>());
    }
}

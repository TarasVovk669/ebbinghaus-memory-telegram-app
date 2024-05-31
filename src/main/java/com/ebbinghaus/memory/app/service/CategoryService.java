package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.model.CategoryMessageCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;

public interface CategoryService {

  Page<CategoryMessageCount> getCategories(Long userId, int page, int size, Sort sort);

  Map<String, Category> existingCategories(Set<Category> categoriesList, Long ownerId);
}

package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.model.CategoryMessageCount;
import com.ebbinghaus.memory.app.model.CategoryMessageCountProj;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CategoryService {

    Page<CategoryMessageCount> getCategories(Long userId, int page, int size, Sort sort);

    Map<String, Category> existingCategories(Set<String> categoriesName, Long ownerId);

    Category save(Category category);

    List<CategoryMessageCountProj> findCategoryMessageCounts(Long ownerId, List<Long> ids);

    void deleteById(Collection<Long> ids, Long ownerId);
}

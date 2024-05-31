package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.model.CategoryDto;
import com.ebbinghaus.memory.app.model.CategoryMessageCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    //List<Category> findAllByOwnerIdAndNameIn(Long ownerId, Collection<String> name);

    List<CategoryDto> findAllByOwnerIdAndNameIn(Long ownerId, Collection<String> name);

    @Query("SELECT new com.ebbinghaus.memory.app.model.CategoryMessageCount(c.id, c.name, COUNT(m)) " +
            "FROM Category c " +
            "LEFT JOIN c.messages m " +
            "WHERE c.ownerId = :ownerId " +
            "GROUP BY c.id, c.name " +
            "ORDER BY (CASE WHEN c.name = '#uncategorized' THEN 0 ELSE 1 END), c.name")
    Page<CategoryMessageCount> findCategoryMessageCounts(@Param("ownerId") Long ownerId, Pageable pageable);
}

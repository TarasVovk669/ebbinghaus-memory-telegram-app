package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.Category;
import com.ebbinghaus.memory.app.model.CategoryDto;
import com.ebbinghaus.memory.app.model.CategoryMessageCount;
import com.ebbinghaus.memory.app.model.proj.CategoryMessageCountProj;
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

    @Query("""
            select new com.ebbinghaus.memory.app.model.CategoryDto
            (c.id, c.name, c.ownerId, c.createdDateTime, c.updatedDateTime)
            from Category c
            where c.ownerId = :ownerId and c.name in (:names)
            """)
    List<CategoryDto> findAllByOwnerIdAndNameIn(Long ownerId, Collection<String> names);

    @Query("""
            SELECT new com.ebbinghaus.memory.app.model.CategoryMessageCount(c.id, c.name, COUNT(m))
            FROM Category c
            LEFT JOIN c.messageCategories m
            WHERE c.ownerId = :ownerId
            GROUP BY c.id, c.name
            ORDER BY (CASE  WHEN c.name = '#uncategorized' THEN 0  WHEN c.name = '#forwarded' THEN 1  ELSE 2 END), c.name
            """)
    Page<CategoryMessageCount> findCategoryMessageCounts(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query(value = """
            select c.id as id, c.name as name, count(mc.category_id) as msgQuantity
            from e_category c
            left join message_category mc on c.id = mc.category_id
            where c.owner_id=:ownerId
              and c.id in (:ids)
            group by c.id, c.name
            """, nativeQuery = true)
    List<CategoryMessageCountProj> findCategoryMessageCounts(@Param("ownerId") Long ownerId, Collection<Long> ids);
}

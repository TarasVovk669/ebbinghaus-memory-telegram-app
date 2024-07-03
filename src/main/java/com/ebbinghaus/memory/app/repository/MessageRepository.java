package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.model.proj.DataMessageCategoryProj;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<EMessage, Long> {

    Page<EMessage> getAllByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT m FROM EMessage m JOIN m.messageCategories c WHERE m.ownerId=:ownerId AND c.category.id = :categoryId")
    Page<EMessage> getAllByOwnerIdAndCategories(Long ownerId, Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"messageCategories", "messageEntities"})
    Optional<EMessage> getEMessageById(Long id);

    @EntityGraph(attributePaths = {"messageCategories", "messageEntities"})
    Optional<EMessage> getEMessageByMessageIdAndOwnerId(Long messageId, Long ownerId);

    @Query(value = """
                        WITH message_counts AS (SELECT owner_id, COUNT(*) AS message_count
                                    FROM e_message
                                    WHERE owner_id = :ownerId
                                    GROUP BY owner_id),
                 category_counts AS (SELECT owner_id, COUNT(*) AS category_count
                                     FROM e_category
                                     WHERE owner_id = :ownerId
                                     GROUP BY owner_id)
            SELECT COALESCE(m.message_count, 0)  AS messageCount,
                   COALESCE(c.category_count, 0) AS categoryCount
            FROM (SELECT :ownerId AS owner_id) owner
                     LEFT JOIN message_counts m ON owner.owner_id = m.owner_id
                     LEFT JOIN category_counts c ON owner.owner_id = c.owner_id
                        """, nativeQuery = true)
    DataMessageCategoryProj getMessageAndCategoryCount(Long ownerId);
}

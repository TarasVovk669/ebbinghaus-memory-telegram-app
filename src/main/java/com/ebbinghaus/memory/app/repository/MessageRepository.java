package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.model.proj.DataMessageCategoryProj;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<EMessage, Long> {

    Page<EMessage> getAllByOwnerId(Long ownerId, Pageable pageable);

    @Query(
            "SELECT m FROM EMessage m JOIN m.messageCategories c WHERE m.ownerId=:ownerId AND c.category.id = :categoryId")
    Page<EMessage> getAllByOwnerIdAndCategories(Long ownerId, Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"messageCategories", "messageEntities"})
    Optional<EMessage> getEMessageById(Long id);

    @EntityGraph(attributePaths = {"messageCategories", "messageEntities"})
    Optional<EMessage> getEMessageByMessageIdAndOwnerId(Long messageId, Long ownerId);

    @Query("SELECT m FROM EMessage m JOIN m.messageEntities WHERE m.ownerId = :ownerId AND LENGTH(m.text) > :length AND m.nextExecutionDateTime < :beforeDate")
    List<EMessage> findCandidateMessages(@Param("ownerId") Long ownerId,
                                         @Param("beforeDate") LocalDateTime beforeDate,
                                         @Param("length") int length,
                                         Pageable pageable);

    @Query(value = """
            SELECT m
            FROM EMessage m
            LEFT JOIN Quiz q ON q.messageId = m.id AND q.ownerId = :ownerId
            join m.messageEntities
            WHERE m.ownerId = :ownerId
              AND LENGTH(m.text) > :length
              AND NOT EXISTS (
                  SELECT 1
                  FROM Quiz recent_q
                  WHERE recent_q.messageId = m.id
                    AND recent_q.ownerId = :ownerId
                    AND recent_q.createdDateTime > :sinceTime
              )
            GROUP BY m.id
            ORDER BY COUNT(q.id) ASC, MAX(q.finishedDateTime) ASC NULLS FIRST, m.createdDateTime ASC
            """)
    List<EMessage> findCandidateMessages(@Param("ownerId") Long ownerId,
                                         @Param("length") int length,
                                         @Param("sinceTime") LocalDateTime sinceTime,
                                         Pageable pageable);

    @Query(
            value =
                    """
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
                            """,
            nativeQuery = true)
    DataMessageCategoryProj getMessageAndCategoryCount(Long ownerId);
}

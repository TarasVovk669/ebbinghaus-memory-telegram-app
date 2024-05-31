package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<EMessage, Long> {

  Page<EMessage> getAllByOwnerId(Long ownerId, Pageable pageable);

  //@EntityGraph(attributePaths = {"messageEntities"})
  @Query("SELECT m FROM EMessage m JOIN m.categories c WHERE m.ownerId=:ownerId AND c.id = :categoryId")
  Page<EMessage> getAllByOwnerIdAndCategories(Long ownerId, Long categoryId , Pageable pageable);

  @EntityGraph(attributePaths = {"categories", "messageEntities"})
  Optional<EMessage> getEMessageById(Long id);

  @EntityGraph(attributePaths = {"categories", "messageEntities"})
  Optional<EMessage> getEMessageByMessageIdAndOwnerId(Long messageId, Long ownerId);
}

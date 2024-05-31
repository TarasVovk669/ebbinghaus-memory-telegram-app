package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageEntityRepository extends JpaRepository<EMessageEntity, Long> {

  @Query(value = "select * from e_message_entity where e_message_id in (:messages_ids)", nativeQuery = true)
  List<EMessageEntity> getAllByIdsIn(@Param("messages_ids") List<Long> messagesIds);

}

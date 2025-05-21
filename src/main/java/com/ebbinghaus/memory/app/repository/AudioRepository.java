package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.audio.AudioText;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioRepository extends JpaRepository<AudioText, Long> {

    @Query("SELECT COUNT(r) FROM AudioText r WHERE r.userId = :userId AND r.createdAt > :since")
    long countRecent(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}

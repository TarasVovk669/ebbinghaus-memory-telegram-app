package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Quiz getFirstByOwnerIdAndMessageIdOrderByIdDesc(Long ownerId, Long messageId);

    Page<Quiz> getAllByOwnerIdOrderByIdDesc(Long ownerId, Pageable pageable);

    @Query("SELECT q FROM Quiz q WHERE q.ownerId = :userId AND (q.createdDateTime > :cutoffTime OR q.finishedDateTime > :cutoffTime)")
    List<Quiz> findAllRecentQuizzesByUserId(@Param("userId") Long userId, @Param("cutoffTime") LocalDateTime cutoffTime);
}

package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import com.ebbinghaus.memory.app.model.proj.QuizCountProj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Quiz getFirstByOwnerIdAndMessageIdOrderByIdDesc(Long ownerId, Long messageId);

    @Query("SELECT count(q) FROM Quiz q WHERE q.ownerId = :userId AND (q.createdDateTime BETWEEN :cutoffTime AND :now OR q.finishedDateTime BETWEEN :cutoffTime AND :now)")
    Long findAllRecentQuizzesByUserId(@Param("userId") Long userId, @Param("cutoffTime") LocalDateTime cutoffTime, @Param("now") LocalDateTime now);

    @Query("""
                SELECT
                COUNT(CASE WHEN q.status = 'FINISHED' THEN 1 END) AS totalFinishedQuizCount,
                COUNT(CASE WHEN q.createdDateTime BETWEEN :cutoffTime AND :now
                        OR q.finishedDateTime BETWEEN :cutoffTime AND :now THEN 1 END) AS availableQuizCount
            FROM
                Quiz q
            WHERE
                q.ownerId = :ownerId
            """)
    QuizCountProj getQuizCount(@Param("ownerId") Long ownerId,
                               @Param("cutoffTime") LocalDateTime cutoffTime,
                               @Param("now") LocalDateTime now);
}

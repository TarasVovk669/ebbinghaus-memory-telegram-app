package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import com.ebbinghaus.memory.app.domain.quiz.QuizQuestion;
import com.ebbinghaus.memory.app.model.proj.QuizQuestionProj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    Optional<QuizQuestion> findFirstByQuizIdAndStatusIsNullOrderById(Long quizId);

    @Query("SELECT " +
            "COUNT(q) AS totalQuestions, " +
            "SUM(CASE WHEN q.status IS NOT NULL THEN 1 ELSE 0 END) AS answeredQuestions, " +
            "SUM(CASE WHEN q.status = 'CORRECT' THEN 1 ELSE 0 END) AS correctQuestions " +
            "FROM QuizQuestion q WHERE q.quizId = :quizId")
    QuizQuestionProj findQuizStatisticsByQuizId(@Param("quizId") Long quizId);

}

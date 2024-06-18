package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

}

package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import com.ebbinghaus.memory.app.repository.QuizRepository;
import com.ebbinghaus.memory.app.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizServiceImpl.class);

    private final QuizRepository quizRepository;

    @Override
    public Quiz createQuiz() {
        return null;
    }
}

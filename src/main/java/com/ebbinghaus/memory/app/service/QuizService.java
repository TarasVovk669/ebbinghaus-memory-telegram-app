package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import com.ebbinghaus.memory.app.model.InputUserData;

public interface QuizService {

    void process(InputUserData userData);

    void answeredQuestion(InputUserData userData);
    void getNextQuestion(InputUserData userData, Long quizId);

    Quiz createQuiz();
}

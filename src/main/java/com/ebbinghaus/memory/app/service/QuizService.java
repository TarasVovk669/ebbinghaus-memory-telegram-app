package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.QuizCount;

public interface QuizService {

    void process(InputUserData userData);

    void answeredQuestion(InputUserData userData);
    void getNextQuestion(InputUserData userData, Long quizId);

    QuizCount countQuizzes(Long id);
}

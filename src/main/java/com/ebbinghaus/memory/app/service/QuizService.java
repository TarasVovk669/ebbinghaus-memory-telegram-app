package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.quiz.Quiz;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.UserState;

import java.util.Collection;
import java.util.Set;

public interface QuizService {

  Quiz createQuiz();

    void process(InputUserData userData);
}

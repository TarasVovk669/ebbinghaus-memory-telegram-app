package com.ebbinghaus.memory.app.model;

import com.ebbinghaus.memory.app.domain.quiz.Quiz;

public record QuizTuple(QuizManageStatus status, Quiz quiz) {}

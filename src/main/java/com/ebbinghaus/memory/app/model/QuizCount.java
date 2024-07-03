package com.ebbinghaus.memory.app.model;

public record QuizCount(
    Long availableQuizCount, Long totalFinishedQuizCount, int totalCountPerDay) {}

package com.ebbinghaus.memory.app.model;

import lombok.Data;

import java.util.List;

@Data
public class QuestionsWrapper {
    private List<QuizQuestionDto> questions;
    private QuizManageStatus error;
}

package com.ebbinghaus.memory.app.model;

import com.ebbinghaus.memory.app.domain.quiz.QuizQuestion;
import java.util.List;

public record AiQuestionTuple(QuizManageStatus status, List<QuizQuestion> questions) {

}

package com.ebbinghaus.memory.app.model;

import com.ebbinghaus.memory.app.domain.quiz.QuestionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDto {

    private String text;

    private QuestionType type;

    private Map<String, String> variants;

    @JsonProperty("correct_answer")
    private String correctAnswer;


}

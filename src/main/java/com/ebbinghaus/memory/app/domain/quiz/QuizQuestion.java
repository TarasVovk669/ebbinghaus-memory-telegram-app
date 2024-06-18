package com.ebbinghaus.memory.app.domain.quiz;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "e_quiz_question")
public class QuizQuestion {

    @Id
    @SequenceGenerator(name = "e_quiz_question_seq", sequenceName = "e_quiz_question_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "e_quiz_question_seq")
    private Long id;

    private String text;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    private QuestionStatus status;

    private String variants; //json

    private String correctAnswer; //json

    private String userAnswer; //json

    private LocalDateTime createdDateTime;

    private LocalDateTime finishedDateTime;
}

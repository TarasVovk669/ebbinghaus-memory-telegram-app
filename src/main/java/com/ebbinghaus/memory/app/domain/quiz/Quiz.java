package com.ebbinghaus.memory.app.domain.quiz;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.Accessors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "e_quiz")
public class Quiz {

    @Id
    @SequenceGenerator(name = "e_quiz_seq", sequenceName = "e_quiz_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "e_quiz_seq")
    private Long id;

    private Long ownerId;

    private Long messageId;

    private LocalDateTime createdDateTime;

    private LocalDateTime finishedDateTime;

    @Enumerated(EnumType.STRING)
    private QuizStatus status;

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "quiz_id")
    private List<QuizQuestion> questions = new ArrayList<>();


}

package com.ebbinghaus.memory.app.domain.quiz;

import com.ebbinghaus.memory.app.domain.EMessageCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "quiz_id")
    private List<QuizQuestion> questions = new ArrayList<>();


}

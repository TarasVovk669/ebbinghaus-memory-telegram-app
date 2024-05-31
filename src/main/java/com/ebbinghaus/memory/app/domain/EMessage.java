package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "e_message")
public class EMessage {

    @Id
    @SequenceGenerator(name = "e_message_seq", sequenceName = "e_message_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "e_message_seq")
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String text;

    private Long ownerId;

    private Long messageId;

    @NotNull
    private Integer executionStep;

    @Embedded
    private File file;

    @ManyToMany(
            fetch = FetchType.LAZY
            , cascade = CascadeType.ALL
    )
    @JoinTable(
            name = "message_category",
            joinColumns = @JoinColumn(name = "message_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "e_message_id")
    private Set<EMessageEntity> messageEntities = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    private LocalDateTime updatedDateTime;

    private LocalDateTime nextExecutionDateTime;
}

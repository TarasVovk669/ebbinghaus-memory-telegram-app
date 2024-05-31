package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "e_message_entity")
public class EMessageEntity {

    @Id
    @SequenceGenerator(name = "e_message_entity_seq", sequenceName = "e_message_entity_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "e_message_entity_seq")
    private Long id;

    @NotNull
    private String value;

    @Column(name = "e_message_id")
    private Long messageId;

}

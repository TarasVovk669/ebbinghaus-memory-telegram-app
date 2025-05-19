package com.ebbinghaus.memory.app.domain.audio;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "e_audio_text")
public class AudioText {

    @Id
    @SequenceGenerator(name = "e_audio_text_seq", sequenceName = "e_audio_text_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "e_audio_text_seq")
    private Long id;
    private Long userId;
    private Long messageId;
    private String description;
    private LocalDateTime createdAt;
}

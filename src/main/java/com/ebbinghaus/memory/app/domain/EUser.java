package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "e_user")
public class EUser {

  @Id private Long id;

  private String languageCode;

  @Enumerated(EnumType.STRING)
  private UserStatus status;

  private Boolean quizRemainderEnabled;

  @CreationTimestamp private LocalDateTime createdDateTime;
}

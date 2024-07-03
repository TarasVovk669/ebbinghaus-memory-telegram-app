package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;

@Data
@ToString(exclude = {"messageCategories"})
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

  @NotNull private Integer executionStep;

  @NotNull
  @Enumerated(EnumType.STRING)
  private EMessageType type;

  @Embedded private File file;

  @OneToMany(cascade = {CascadeType.REMOVE})
  @JoinColumn(name = "message_id")
  private Set<EMessageCategory> messageCategories = new HashSet<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "e_message_id")
  private Set<EMessageEntity> messageEntities = new HashSet<>();

  @CreationTimestamp private LocalDateTime createdDateTime;

  @UpdateTimestamp private LocalDateTime updatedDateTime;

  private LocalDateTime nextExecutionDateTime;

  public void setMessageEntities(Set<EMessageEntity> messageEntities) {
    this.messageEntities.clear();
    if (messageEntities != null) {
      this.messageEntities.addAll(messageEntities);
    }
  }

  public void setMessageEntitiesDirectly(Set<EMessageEntity> messageEntities) {
    this.messageEntities = messageEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EMessage eMessage = (EMessage) o;
    return Objects.equals(id, eMessage.id) && Objects.equals(ownerId, eMessage.ownerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, ownerId);
  }
}

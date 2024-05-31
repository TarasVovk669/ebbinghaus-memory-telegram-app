package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "e_category")
public class Category {

  @Id
  @SequenceGenerator(name = "e_category_seq", sequenceName = "e_category_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "e_category_seq")
  private Long id;

  private String name;

  private Long ownerId;

  @ManyToMany(fetch = FetchType.LAZY, mappedBy = "categories")
  private Set<EMessage> messages = new HashSet<>();

  @CreationTimestamp private LocalDateTime createdDateTime;

  @UpdateTimestamp private LocalDateTime updatedDateTime;
}

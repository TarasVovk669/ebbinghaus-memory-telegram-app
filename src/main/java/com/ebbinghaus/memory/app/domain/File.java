package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class File {

  @Column(name = "file_id")
  private String fileId;

  @Enumerated(EnumType.STRING)
  @Column(name = "file_type")
  private FileType fileType;
}

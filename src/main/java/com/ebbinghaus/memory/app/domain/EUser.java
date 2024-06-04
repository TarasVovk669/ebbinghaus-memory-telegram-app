package com.ebbinghaus.memory.app.domain;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Id
    private Long id;

    private String languageCode;

    @CreationTimestamp
    private LocalDateTime createdDateTime;

}

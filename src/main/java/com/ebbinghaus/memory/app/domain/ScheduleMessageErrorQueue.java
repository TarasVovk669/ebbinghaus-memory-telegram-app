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
@Table(name = "schedule_message_error_queue")
public class ScheduleMessageErrorQueue {

    @Id
    private Long messageId;

    private Long chatId;

    private Long ownerId;

    private String errorText;

    private LocalDateTime time;

}

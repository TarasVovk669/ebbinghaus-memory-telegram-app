package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

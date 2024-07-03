package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

package com.ebbinghaus.memory.app.domain.embedded;

import com.ebbinghaus.memory.app.model.UserState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class EMessageStateId implements Serializable {

    private Long userId;

    private Long chatId;

    @Enumerated(EnumType.STRING)
    private UserState state;
}

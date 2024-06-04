package com.ebbinghaus.memory.app.domain;

import com.ebbinghaus.memory.app.model.UserState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "e_user_state")
public class EUserState {

    @Id
    private Long userId;

    @Enumerated(EnumType.STRING)
    private UserState state;


}

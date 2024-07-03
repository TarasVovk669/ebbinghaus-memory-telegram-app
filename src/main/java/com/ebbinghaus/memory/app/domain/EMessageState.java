package com.ebbinghaus.memory.app.domain;

import com.ebbinghaus.memory.app.domain.embedded.EMessageStateId;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "e_message_state")
public class EMessageState {

    @EmbeddedId
    private EMessageStateId id;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Integer> messageIds = new HashSet<>();
}

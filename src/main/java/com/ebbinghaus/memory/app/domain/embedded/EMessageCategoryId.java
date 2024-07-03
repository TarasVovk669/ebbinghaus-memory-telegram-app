package com.ebbinghaus.memory.app.domain.embedded;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class EMessageCategoryId implements Serializable {

    private Long messageId;

    private Long categoryId;

}

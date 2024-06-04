package com.ebbinghaus.memory.app.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class EMessageCategoryId implements Serializable {

    private Long messageId;

    private Long categoryId;

}

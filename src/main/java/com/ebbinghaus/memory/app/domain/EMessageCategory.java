package com.ebbinghaus.memory.app.domain;

import com.ebbinghaus.memory.app.domain.embedded.EMessageCategoryId;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.*;

@Data
@ToString(exclude = {"message", "category"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "message_category")
public class EMessageCategory {

  @EmbeddedId private EMessageCategoryId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("messageId")
  private EMessage message;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("categoryId")
  private Category category;

  public EMessageCategory(EMessage message, Category category) {
    this.message = message;
    this.category = category;
    this.id = new EMessageCategoryId(message.getId(), category.getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EMessageCategory that = (EMessageCategory) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

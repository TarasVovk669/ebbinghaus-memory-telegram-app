package com.ebbinghaus.memory.app.model;

import java.time.LocalDateTime;

public record CategoryDto(Long id, String name, Long ownerId, LocalDateTime createdDateTime,
                          LocalDateTime updatedDateTime) {
}

package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.model.AudioCount;

public interface AudioService {
    boolean canGenerate(Long userId);

    void save(Long userId, Long messageId, String audioId);

    AudioCount count(Long userId);
}

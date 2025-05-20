package com.ebbinghaus.memory.app.service;

public interface AudioService {
    boolean canGenerate(Long userId);

    void save(Long userId, Long messageId, String audioId);
}

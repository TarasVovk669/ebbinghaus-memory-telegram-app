package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.audio.AudioText;
import com.ebbinghaus.memory.app.repository.AudioRepository;
import com.ebbinghaus.memory.app.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;

@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {

  private static final Logger log = LoggerFactory.getLogger(AudioServiceImpl.class);

  private final AudioRepository audioRepository;

  @Override
  public boolean canGenerate(Long userId) {
    var since = LocalDateTime.now(UTC).minusHours(24);
    return audioRepository.countRecent(userId, since) < 2;
  }

  @Override
  public void save(Long userId, Long messageId, Long audioId) {
    log.info("Saving audio with id: {} for user: {}", audioId, userId);
    audioRepository.save(
        AudioText.builder()
            .userId(userId)
            .messageId(messageId)
            .createdAt(LocalDateTime.now(UTC))
            .build());
  }
}

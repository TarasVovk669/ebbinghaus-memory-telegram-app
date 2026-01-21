package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.model.ExplainItBackCount;

public interface ExplainItBackService {

  boolean canExplain(Long userId);

  ExplainItBackCount count(Long userId);

  void processExplain(
      Long userId,
      Long chatId,
      Long messageId,
      String voiceFileId,
      String languageCode,
      Integer receiptMessageId,
      Integer replyMessageId);
}

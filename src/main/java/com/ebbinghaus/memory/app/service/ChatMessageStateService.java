package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.model.UserState;
import java.util.Collection;
import java.util.Set;

public interface ChatMessageStateService {

  void addMessage(Long userId, Long chatId, UserState state, Collection<Integer> messageId);

  Set<Integer> getMessages(Long userId, Long chatId, UserState state);

  void clearStateMessages(Long userId, Long chatId, UserState state);
}

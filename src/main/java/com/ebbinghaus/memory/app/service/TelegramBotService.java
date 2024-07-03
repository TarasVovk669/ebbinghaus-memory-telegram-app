package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.model.InputUserData;

public interface TelegramBotService {
  void processEditMessageCallback(String command, InputUserData inputUserData);

  void processButtonMessageCallback(String command, InputUserData inputUserData);

  void processTextInputCallback(InputUserData inputUserData);
}

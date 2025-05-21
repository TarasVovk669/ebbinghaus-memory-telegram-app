package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.EUser;
import com.ebbinghaus.memory.app.model.UserState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;

@Service
public interface UserService {

  UserState getUserState(Long userId);

  void setUserState(Long userId, UserState state);

  void addUser(User user);

  EUser getUser(Long userId);

  Optional<EUser> findUser(Long userId);

  void updateLanguageCode(Long userId, String languageCode);
}

package com.ebbinghaus.memory.app.service.impl;

import static com.ebbinghaus.memory.app.utils.Constants.AVAILABLE_LANGUAGES_MAP;
import static com.ebbinghaus.memory.app.utils.Constants.DEFAULT_LANGUAGE_CODE;
import static java.time.ZoneOffset.UTC;

import com.ebbinghaus.memory.app.domain.EUser;
import com.ebbinghaus.memory.app.domain.EUserState;
import com.ebbinghaus.memory.app.model.UserState;
import com.ebbinghaus.memory.app.repository.UserRepository;
import com.ebbinghaus.memory.app.repository.UserStateRepository;
import com.ebbinghaus.memory.app.service.UserService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserStateRepository userStateRepository;

    @Override
    public void addUser(User user) {
        log.info("Add user with id: {}", user.getId());

        userRepository.save(
                EUser.builder()
                        .id(user.getId())
                        .createdDateTime(LocalDateTime.now(UTC))
                        .languageCode(
                                AVAILABLE_LANGUAGES_MAP.containsKey(user.getLanguageCode())
                                        ? user.getLanguageCode()
                                        : DEFAULT_LANGUAGE_CODE)
                        .build());
    }

    @Override
    @Cacheable(value = "get_user", key = "#userId")
    public EUser getUser(Long userId) {
        log.info("Get user with id: {} ", userId);
        return userRepository.findById(userId)
                .orElseGet(() -> userRepository.save(EUser
                        .builder()
                        .id(userId)
                        .languageCode(DEFAULT_LANGUAGE_CODE)
                        .createdDateTime(LocalDateTime.now(UTC))
                        .build()));

    }

    @Override
    @Cacheable(value = "get_user_optional", key = "#userId")
    public Optional<EUser> findUser(Long userId) {
        log.info("Get user with id: {} ", userId);
        return userRepository.findById(userId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "get_user", key = "#userId"),
            @CacheEvict(value = "get_user_optional", key = "#userId")})
    public void updateLanguageCode(Long userId, String languageCode) {
        log.info("Update language code: {} for user_id: {}", languageCode, userId);
        userRepository.findById(userId)
                .ifPresent(user -> {
                    user.setLanguageCode(languageCode);
                    userRepository.save(user);
                });
    }

    @Override
    @Cacheable(value = "get_user_state", key = "#userId")
    public UserState getUserState(Long userId) {
        log.info("Get user_state with id: {}", userId);

        return userStateRepository.findById(userId)
                .map(EUserState::getState)
                .orElse(UserState.DEFAULT);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "get_user_state", key = "#userId")})
    public void setUserState(Long userId, UserState state) {
        log.info("Add user_state with id: {} and state: {}", userId, state);

        userStateRepository.save(EUserState.builder()
                .userId(userId)
                .state(state)
                        .dateTime(LocalDateTime.now(UTC))
                .build());
    }
}

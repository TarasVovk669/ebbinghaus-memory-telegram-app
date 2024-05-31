package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.EUser;
import com.ebbinghaus.memory.app.model.UserState;
import com.ebbinghaus.memory.app.repository.UserRepository;
import com.ebbinghaus.memory.app.service.UserService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.ebbinghaus.memory.app.utils.Constants.AVAILABLE_LANGUAGES_MAP;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Map<Long, UserState> USER_STATE_MAP = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    public static final String DEFAULT_LANGUAGE_CODE = "en";

    private final UserRepository userRepository;

    @Override
    public UserState getUserState(Long userId) {
        log.info("Get user_state with id: {}", userId);
        return USER_STATE_MAP.get(userId);
    }

    @Override
    public void setUserState(Long userId, UserState state) {
        log.info("Add user with id: {} and state: {}", userId, state);
        USER_STATE_MAP.put(userId, state);
    }


    @Override
    public void addUser(User user) {
        log.info("Add user with id: {}", user.getId());

        userRepository.save(
                EUser.builder().id(user.getId())
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
        return userRepository.findById(userId).orElseThrow();
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
        userRepository.updateUserLanguageCode(userId, languageCode);
    }
}

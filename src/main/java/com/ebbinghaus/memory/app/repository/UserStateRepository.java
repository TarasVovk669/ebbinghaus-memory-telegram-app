package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EUserState;
import com.ebbinghaus.memory.app.model.UserState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStateRepository extends JpaRepository<EUserState, Long> {

    void deleteAllByUserIdAndState(Long userId, UserState state);
}

package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EUser;
import com.ebbinghaus.memory.app.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<EUser, Long> {
    List<EUser> findAllByStatus(UserStatus status);
}

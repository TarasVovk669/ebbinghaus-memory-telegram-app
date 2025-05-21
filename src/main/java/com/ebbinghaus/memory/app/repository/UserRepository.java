package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<EUser, Long> {}

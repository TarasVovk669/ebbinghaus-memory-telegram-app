package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<EUser, Long> {


    @Modifying
    @Query("update EUser u set u.languageCode =:language_code where u.id=:user_id")
    void updateUserLanguageCode(@Param("user_id") Long userId, @Param("language_code") String languageCode);
}

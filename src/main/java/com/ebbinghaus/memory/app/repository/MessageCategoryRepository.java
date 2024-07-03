package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EMessageCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageCategoryRepository extends JpaRepository<EMessageCategory, Long> {}

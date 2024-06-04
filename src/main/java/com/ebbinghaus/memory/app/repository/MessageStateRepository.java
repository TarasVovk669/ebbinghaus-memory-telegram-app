package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.domain.EMessageState;
import com.ebbinghaus.memory.app.domain.embedded.EMessageStateId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageStateRepository extends JpaRepository<EMessageState, EMessageStateId> {

}

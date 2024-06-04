package com.ebbinghaus.memory.app.repository;

import com.ebbinghaus.memory.app.domain.EMessageState;
import com.ebbinghaus.memory.app.domain.embedded.EMessageStateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageStateRepository extends JpaRepository<EMessageState, EMessageStateId> {

}

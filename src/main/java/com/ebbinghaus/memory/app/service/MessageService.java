package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.EMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public interface MessageService {

    EMessage addMessage(EMessage message);

    EMessage updateMessage(EMessage message);

    Page<EMessage> getMessages(Long userId, Long categoryId, int page, int size, Sort sort);

    EMessage getMessage(Long id, boolean fetch);

    EMessage getUpdatedMessage(Long id, boolean fetch);

    EMessage getMessageByTgExternalId(Long externalId, Long userId);

    void deleteMessage(Long id);
}

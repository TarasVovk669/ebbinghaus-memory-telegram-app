package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.model.MessageTuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

public interface MessageService {

    EMessage addMessage(MessageTuple message);

    EMessage updateMessage(MessageTuple message);

    Page<EMessage> getMessages(Long userId, Long categoryId, int page, int size, Sort sort);

    EMessage getMessage(Long id, boolean fetch);

    EMessage getUpdatedMessage(Long id, boolean fetch);

    EMessage getMessageByTgExternalId(Long externalId, Long userId);

    void deleteMessage(Long id);
}

package com.ebbinghaus.memory.app.model;

import com.ebbinghaus.memory.app.domain.File;
import com.ebbinghaus.memory.app.service.*;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;

@Data
@Builder
@ToString
public class InputUserData {

    private File file;
    private User user;
    private Long chatId;
    private UserState state;
    private String ownerName;
    private Integer messageId;
    private String messageText;
    private String languageCode;
    private MessageType messageType;
    private boolean isForwardedMessage;
    private Map<String, String> callBackData;
    private List<MessageEntity> messageEntities;







}

package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.exception.TelegramCallException;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.model.MessageType;
import com.ebbinghaus.memory.app.service.MessageSourceService;
import com.ebbinghaus.memory.app.service.TelegramClientService;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.ebbinghaus.memory.app.utils.Constants.MARKDOWN;
import static com.ebbinghaus.memory.app.utils.Constants.MESSAGE_CAN_T_BE_DELETED_FOR_EVERYONE;
import static com.ebbinghaus.memory.app.utils.MessageUtils.manageMsgType;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTryTgCall;

@Service
@RequiredArgsConstructor
public class TelegramClientServiceImpl implements TelegramClientService {

    private static final Logger log = LoggerFactory.getLogger(TelegramClientServiceImpl.class);

    private static final String METRIC_SEND_MESSAGE = "bot.telegram.sendMessage";
    private static final String METRIC_EDIT_MESSAGE = "bot.telegram.sendEditMessage";
    private static final String METRIC_SEND_PHOTO = "bot.telegram.sendPhoto";
    private static final String METRIC_SEND_AUDIO = "bot.telegram.sendAudio";
    private static final String METRIC_DOWNLOAD_FILE = "bot.telegram.downloadFile";
    private static final String METRIC_DELETE_MESSAGE = "bot.telegram.deleteMessage";
    private static final String METRIC_DELETE_MESSAGES = "bot.telegram.deleteMessages";

    private final TelegramClient telegramClient;
    private final MessageSourceService messageSourceService;
    private final MeterRegistry meterRegistry;

    // ----------- API (send/edit) -----------

    @Override
    @Retry(name = "telegram")
    public Message sendMessage(MessageType messageType, MessageDataRequest request) {
        return record(METRIC_SEND_MESSAGE, () -> messageType.sendMessage(request, telegramClient));
    }

    @Override
    @Retry(name = "telegram")
    public void sendEditMessage(MessageType messageType, MessageDataRequest request) {
        record(METRIC_EDIT_MESSAGE, () -> messageType.editMessage(request, telegramClient));
    }

    @Override
    @Retry(name = "telegram")
    public void sendEditMessage(EditMessageText editMessage) {
        record(METRIC_EDIT_MESSAGE, () -> doTry(() -> telegramClient.execute(editMessage)));
    }

    @Override
    public Message sendMessage(Long chatId, String text) {
        return sendMessage(chatId, text, null);
    }

    @Override
    public Message sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        return sendMessage(chatId, text, replyKeyboard, null);
    }

    @Override
    public Message sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard, List<MessageEntity> entities) {
        return sendMessage(chatId, text, replyKeyboard, entities, null);
    }

    @Override
    @Retry(name = "telegram")
    public Message sendMessage(
            Long chatId, String text, ReplyKeyboard replyKeyboard, List<MessageEntity> entities, Long replyMessageId) {
        return record(
                METRIC_SEND_MESSAGE,
                () -> doTry(() -> telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .parseMode((entities == null || entities.isEmpty()) ? MARKDOWN : null)
                        .replyMarkup(replyKeyboard)
                        .entities(entities)
                        .replyToMessageId(replyMessageId != null ? replyMessageId.intValue() : null)
                        .build())));
    }

    @Override
    @Retry(name = "telegram")
    public void sendEditMessage(
            Long chatId,
            String text,
            InlineKeyboardMarkup replyKeyboard,
            List<MessageEntity> entities,
            Integer messageId) {
        record(
                METRIC_EDIT_MESSAGE,
                () -> doTry(() -> telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(text)
                        .parseMode((entities == null || entities.isEmpty()) ? MARKDOWN : null)
                        .replyMarkup(replyKeyboard)
                        .entities(entities)
                        .build())));
    }

    @Override
    @Retry(name = "telegram")
    public Message sendPhotoMessage(Long chatId, String text, ReplyKeyboard replyKeyboard, String url, String fileId) {
        return record(
                METRIC_SEND_PHOTO,
                () -> doTryTgCall(() -> telegramClient.execute(SendPhoto.builder()
                        .chatId(chatId)
                        .caption(text)
                        .parseMode(MARKDOWN)
                        .replyMarkup(replyKeyboard)
                        .photo(fileId != null ? new InputFile(fileId) : new InputFile(new File(url)))
                        .build())));
    }

    @Override
    @Retry(name = "telegram")
    public Message sendAudioMessage(Long chatId, byte[] audioByteArray, Integer replyMessageId) {
        return record(
                METRIC_SEND_AUDIO,
                () -> doTryTgCall(() -> telegramClient.execute(SendVoice.builder()
                        .chatId(chatId)
                        .voice(new InputFile(new ByteArrayInputStream(audioByteArray), "speech.mp3"))
                        .replyToMessageId(replyMessageId)
                        .build())));
    }

    @Override
    @Retry(name = "telegram")
    public byte[] downloadFile(String fileId) {
        return record(
                METRIC_DOWNLOAD_FILE,
                () -> doTryTgCall(() -> {
                    var file = telegramClient.execute(GetFile.builder().fileId(fileId).build());
                    try (var stream = telegramClient.downloadFileAsStream(file)) {
                        return stream.readAllBytes();
                    }
                }));
    }

    // ----------- API (delete) - unified -----------

    @Override
    @Retry(name = "telegram")
    public void deleteMessage(Long chatId, int messageId) {
        deleteMessageCore(chatId, messageId, null);
    }

    @Override
    @Retry(name = "telegram")
    public void deleteMessage(Long chatId, int messageId, String languageCode) {
        deleteMessageCore(
                chatId,
                messageId,
                () -> MessageType.SMPL.editMessage(
                        MessageDataRequest.builder()
                                .chatId(chatId)
                                .messageText(messageSourceService.getMessage(
                                        "messages.error.tg_msg_not_allow_delete", languageCode))
                                .messageId(messageId)
                                .build(),
                        telegramClient));
    }

    @Override
    @Retry(name = "telegram")
    public void deleteMessage(Long chatId, int messageId, String languageCode, EMessage message) {
        deleteMessageCore(chatId, messageId, () -> {
            manageMsgType(message)
                    .editMessage(
                            MessageDataRequest.builder()
                                    .chatId(chatId)
                                    .messageText(messageSourceService.getMessage(
                                            "messages.error.tg_msg_not_allow_delete", languageCode))
                                    .messageId(messageId)
                                    .file(message.getFile())
                                    .build(),
                            telegramClient);
        });
    }

    @Override
    @Retry(name = "telegram")
    public void deleteMessages(Long chatId, Collection<Integer> messageIds) {
        runTimed(
                METRIC_DELETE_MESSAGES,
                () -> telegramClient.execute(DeleteMessages.builder()
                        .chatId(chatId)
                        .messageIds(messageIds)
                        .build()),
                null);
    }

    private void deleteMessageCore(Long chatId, int messageId, Runnable onCannotDeleteForEveryone) {
        runTimed(
                METRIC_DELETE_MESSAGE,
                () -> telegramClient.execute(DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .build()),
                onCannotDeleteForEveryone);
    }

    private void runTimed(String metric, TgCall call, Runnable onCannotDeleteForEveryone) {
        try {
            record(metric, () -> {
                try {
                    call.run();
                } catch (TelegramApiException e) {
                    throw new TelegramCallException(e.getMessage());
                }
            });
        } catch (TelegramCallException ex) {
            log.warn("Telegram call failed. metric={}, error={}", metric, ex.getMessage());

            if (onCannotDeleteForEveryone != null
                    && ex.getMessage() != null
                    && ex.getMessage().contains(MESSAGE_CAN_T_BE_DELETED_FOR_EVERYONE)) {
                onCannotDeleteForEveryone.run();
            }
        }
    }

    private <T> T record(String name, Supplier<T> supplier) {
        var sample = Timer.start(meterRegistry);
        try {
            return supplier.get();
        } finally {
            sample.stop(Timer.builder(name)
                    .description("Telegram client call latency")
                    .register(meterRegistry));
        }
    }

    private void record(String name, Runnable runnable) {
        var sample = Timer.start(meterRegistry);
        try {
            runnable.run();
        } finally {
            sample.stop(Timer.builder(name)
                    .description("Telegram client call latency")
                    .register(meterRegistry));
        }
    }

    @FunctionalInterface
    private interface TgCall {
        void run() throws TelegramApiException;
    }
}

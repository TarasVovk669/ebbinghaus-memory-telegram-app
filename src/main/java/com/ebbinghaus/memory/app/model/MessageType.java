package com.ebbinghaus.memory.app.model;

import com.ebbinghaus.memory.app.domain.File;
import com.ebbinghaus.memory.app.domain.FileType;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTryTgCall;

public enum MessageType {
    SMPL {
        @Override
        public List<MessageEntity> getMsgEntities(Message message) {
            return message.getEntities();
        }

        @Override
        public String getMsgText(Message message) {
            return message.getText();
        }

        @Override
        public File getFile(Message message) {
            return null;
        }

        @Override
        public Message sendMessage(MessageDataRequest data, TelegramClient telegramClient) {
            return doTry(
                    () ->
                            telegramClient.execute(
                                    SendMessage.builder()
                                            .chatId(data.getChatId())
                                            .text(data.getMessageText())
                                            .parseMode(
                                                    data.getEntities() == null || data.getEntities().isEmpty()
                                                            ? "markdown"
                                                            : null)
                                            .replyMarkup(data.getReplyKeyboard())
                                            .entities(data.getEntities())
                                            .replyToMessageId(
                                                    null != data.getReplyMessageId()
                                                            ? data.getReplyMessageId().intValue()
                                                            : null)
                                            .build()));
        }

        @Override
        public void editMessage(MessageDataRequest data, TelegramClient telegramClient) {
            doTryTgCall(
                    () ->
                            telegramClient.execute(
                                    EditMessageText.builder()
                                            .chatId(data.getChatId())
                                            .messageId(data.getMessageId())
                                            .text(data.getMessageText())
                                            .parseMode(
                                                    data.getEntities() == null || data.getEntities().isEmpty()
                                                            ? "markdown"
                                                            : null)
                                            .replyMarkup(data.getReplyKeyboard())
                                            .entities(data.getEntities())
                                            .build()));
        }

        @Override
        public boolean isAllowedSize(Integer length) {
            return length <= 4096;
        }
    },
    IMG {
        @Override
        public List<MessageEntity> getMsgEntities(Message message) {
            return message.getCaptionEntities();
        }

        @Override
        public String getMsgText(Message message) {
            return message.getCaption();
        }

        @Override
        public File getFile(Message message) {
            return File.builder()
                    .fileId(message.getPhoto().getFirst().getFileId())
                    .fileType(FileType.PHOTO)
                    .build();
        }

        @Override
        public Message sendMessage(MessageDataRequest userData, TelegramClient telegramClient) {
            return doTryTgCall(() ->
                    telegramClient.execute(
                            SendPhoto.builder()
                                    .chatId(userData.getChatId())
                                    .caption(userData.getMessageText())
                                    .captionEntities(userData.getEntities())
                                    .replyMarkup(userData.getReplyKeyboard())
                                    .photo(new InputFile(userData.getFile().getFileId()))
                                    .build()));
        }

        @Override
        public void editMessage(MessageDataRequest data, TelegramClient telegramClient) {
            doTryTgCall(
                    () ->
                            telegramClient.execute(
                                    EditMessageMedia.builder()
                                            .chatId(data.getChatId())
                                            .messageId(data.getMessageId())
                                            .media(
                                                    InputMediaPhoto.builder()
                                                            .caption(data.getMessageText())
                                                            .captionEntities(data.getEntities())
                                                            .media(data.getFile().getFileId())
                                                            .build())
                                            .replyMarkup(data.getReplyKeyboard())
                                            .build()));
        }

        @Override
        public boolean isAllowedSize(Integer length) {
            return length <= 1024;
        }
    },
    DOC {
        @Override
        public List<MessageEntity> getMsgEntities(Message message) {
            return message.getCaptionEntities();
        }

        @Override
        public String getMsgText(Message message) {
            return message.getCaption();
        }

        @Override
        public File getFile(Message message) {
            return File.builder()
                    .fileId(message.getDocument().getFileId())
                    .fileType(FileType.DOCUMENT)
                    .build();
        }

        @Override
        public Message sendMessage(MessageDataRequest userData, TelegramClient telegramClient) {
            return doTryTgCall(
                    () ->
                            telegramClient.execute(
                                    SendDocument.builder()
                                            .chatId(userData.getChatId())
                                            .caption(userData.getMessageText())
                                            .captionEntities(userData.getEntities())
                                            .replyMarkup(userData.getReplyKeyboard())
                                            .document(new InputFile(userData.getFile().getFileId()))
                                            .build()));
        }

        @Override
        public void editMessage(MessageDataRequest data, TelegramClient telegramClient) {
            doTryTgCall(
                    () ->
                            telegramClient.execute(
                                    EditMessageMedia.builder()
                                            .chatId(data.getChatId())
                                            .messageId(data.getMessageId())
                                            .media(
                                                    InputMediaDocument.builder()
                                                            .caption(data.getMessageText())
                                                            .captionEntities(data.getEntities())
                                                            .media(data.getFile().getFileId())
                                                            .build())
                                            .replyMarkup(data.getReplyKeyboard())
                                            .build()));
        }

        @Override
        public boolean isAllowedSize(Integer length) {
            return length <= 1024;
        }
    },
    VIDEO {
        @Override
        public List<MessageEntity> getMsgEntities(Message message) {
            return message.getCaptionEntities();
        }

        @Override
        public String getMsgText(Message message) {
            return message.getCaption();
        }


        @Override
        public File getFile(Message message) {
            return File.builder()
                    .fileId(message.getVideo().getFileId())
                    .fileType(FileType.VIDEO)
                    .build();
        }

        @Override
        public Message sendMessage(MessageDataRequest userData, TelegramClient telegramClient) {
            return doTryTgCall(
                    () ->
                            telegramClient.execute(
                                    SendVideo.builder()
                                            .chatId(userData.getChatId())
                                            .caption(userData.getMessageText())
                                            .captionEntities(userData.getEntities())
                                            .replyMarkup(userData.getReplyKeyboard())
                                            .video(new InputFile(userData.getFile().getFileId()))
                                            .build()));
        }

        @Override
        public void editMessage(MessageDataRequest data, TelegramClient telegramClient) {
            doTryTgCall(
                    () ->
                            telegramClient.execute(
                                    EditMessageMedia.builder()
                                            .chatId(data.getChatId())
                                            .messageId(data.getMessageId())
                                            .media(
                                                    InputMediaDocument.builder()
                                                            .caption(data.getMessageText())
                                                            .captionEntities(data.getEntities())
                                                            .media(data.getFile().getFileId())
                                                            .build())
                                            .replyMarkup(data.getReplyKeyboard())
                                            .build()));
        }

        @Override
        public boolean isAllowedSize(Integer length) {
            return length <= 1024;
        }
    };

    public abstract List<MessageEntity> getMsgEntities(Message message);

    public abstract String getMsgText(Message message);

    public abstract File getFile(Message message);

    public abstract Message sendMessage(MessageDataRequest data, TelegramClient telegramClient);

    public abstract void editMessage(MessageDataRequest data, TelegramClient telegramClient);

    public abstract boolean isAllowedSize(Integer length);
}

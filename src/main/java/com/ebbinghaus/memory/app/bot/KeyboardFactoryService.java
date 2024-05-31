package com.ebbinghaus.memory.app.bot;

import com.ebbinghaus.memory.app.service.MessageSourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;


@Service
@AllArgsConstructor
public class KeyboardFactoryService {

    private final MessageSourceService messageSourceService;

    private final ObjectMapper objectMapper;

    public ReplyKeyboard getMainMenuKeyboard(String languageCode) {
        KeyboardRow row1 = new KeyboardRow(List.of(new KeyboardButton(messageSourceService.getMessage("messages.menu.add-new-info", languageCode))));
        KeyboardRow row2 =
                new KeyboardRow(
                        List.of(
                                new KeyboardButton(messageSourceService.getMessage("messages.menu.data-list", languageCode)),
                                new KeyboardButton(messageSourceService.getMessage("messages.menu.category-list", languageCode)),
                                new KeyboardButton(messageSourceService.getMessage("messages.menu.profile", languageCode))));

        return ReplyKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row1, row2))
                .resizeKeyboard(true)
                .build();
    }

    public InlineKeyboardMarkup getMessageKeyboard(
            Long messageId, String languageCode) {
        var rowInline =
                List.of(
                        InlineKeyboardButton.builder()
                                .text(messageSourceService.getMessage("messages.navigation.view", languageCode))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, VIEW_MESSAGE_CALLBACK),

                                                                        Map.entry(MESSAGE_ID, messageId)))))
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(messageSourceService.getMessage("messages.navigation.edit", languageCode))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, EDIT_MESSAGE_CALLBACK),

                                                                        Map.entry(MESSAGE_ID, messageId)))))
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(messageSourceService.getMessage("messages.navigation.delete", languageCode))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, DELETE_MESSAGE_CALLBACK),
                                                                        Map.entry(MESSAGE_ID, messageId)))))
                                .build());

        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(rowInline)));
    }

    public InlineKeyboardMarkup getViewKeyboard(
            Long messageId, String languageCode) {
        var rowInline =
                List.of(
                        InlineKeyboardButton.builder()
                                .text(messageSourceService.getMessage("messages.navigation.back", languageCode))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, BACK_MESSAGE_CALLBACK),

                                                                        Map.entry(MESSAGE_ID, messageId)))))
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(messageSourceService.getMessage("messages.navigation.edit", languageCode))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, EDIT_MESSAGE_CALLBACK),
                                                                        Map.entry(MESSAGE_ID, messageId)))))
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(messageSourceService.getMessage("messages.navigation.delete", languageCode))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, DELETE_MESSAGE_CALLBACK),
                                                                        Map.entry(MESSAGE_ID, messageId)))))
                                .build());

        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(rowInline)));
    }

    public InlineKeyboardMarkup getDeleteKeyboard(Long messageId, String languageCode) {
        var rowInline =
                List.of(
                        InlineKeyboardButton.builder()
                                .text(messageSourceService.getMessage("messages.delete.confirmation.yes", languageCode))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, DELETE_MESSAGE_YES_CALLBACK),
                                                                        Map.entry(MESSAGE_ID, messageId)))))
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(messageSourceService.getMessage("messages.delete.confirmation.no", languageCode))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, DELETE_MESSAGE_NO_CALLBACK),
                                                                        Map.entry(MESSAGE_ID, messageId)))))
                                .build());

        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(rowInline)));
    }

    public InlineKeyboardMarkup getProfileKeyboard(String languageCode) {
        var changeLanguage = InlineKeyboardButton.builder()
                .text(messageSourceService.getMessage("messages.profile.change-language", languageCode))
                .callbackData(
                        doTry(
                                () ->
                                        objectMapper.writeValueAsString(
                                                Map.ofEntries(
                                                        Map.entry(OPERATION, VIEW_PROFILE_LANGUAGE_CALLBACK)))))
                .build();

        var contactInfo = InlineKeyboardButton.builder()
                .text(messageSourceService.getMessage("messages.profile.contact-info", languageCode))
                .callbackData(
                        doTry(
                                () ->
                                        objectMapper.writeValueAsString(
                                                Map.ofEntries(
                                                        Map.entry(OPERATION, CONTACT_INFO_CALLBACK)))))
                .build();

        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(changeLanguage), new InlineKeyboardRow(contactInfo)));
    }

    public InlineKeyboardMarkup getAvailableLanguage(String languageCode) {
        List<InlineKeyboardRow> list = new ArrayList<>(AVAILABLE_LANGUAGES_MAP.entrySet().stream()
                .filter(e -> !e.getKey().equals(languageCode))
                .map(e -> List.of(
                        InlineKeyboardButton.builder()
                                .text(e.getValue().emoji().concat(" ").concat(e.getValue().name()))
                                .callbackData(
                                        doTry(
                                                () ->
                                                        objectMapper.writeValueAsString(
                                                                Map.ofEntries(
                                                                        Map.entry(OPERATION, CHANGE_PROFILE_LANGUAGE_CALLBACK),
                                                                        Map.entry(LANGUAGE_CODE, e.getKey())
                                                                ))))
                                .build()))
                .map(InlineKeyboardRow::new)
                .toList());

        list.add(new InlineKeyboardRow(List.of(
                InlineKeyboardButton.builder()
                        .text(messageSourceService.getMessage("messages.navigation.back", languageCode))
                        .callbackData(
                                doTry(
                                        () ->
                                                objectMapper.writeValueAsString(
                                                        Map.ofEntries(
                                                                Map.entry(OPERATION, PROFILE_MAIN_MENU_CALLBACK)
                                                        ))))
                        .build())));
        return new InlineKeyboardMarkup(list);
    }

    public InlineKeyboardMarkup getSingleBackKeyboard(String languageCode) {
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(List.of(
                InlineKeyboardButton.builder()
                        .text(messageSourceService.getMessage("messages.navigation.back", languageCode))
                        .callbackData(
                                doTry(
                                        () ->
                                                objectMapper.writeValueAsString(
                                                        Map.ofEntries(
                                                                Map.entry(OPERATION, PROFILE_MAIN_MENU_CALLBACK)
                                                        ))))
                        .build()))));
    }
}

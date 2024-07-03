package com.ebbinghaus.memory.app.service;

import com.ebbinghaus.memory.app.domain.quiz.QuizQuestion;
import com.ebbinghaus.memory.app.model.InputUserData;
import org.springframework.data.domain.Page;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;

public interface KeyboardService {
    ReplyKeyboard getMainMenuKeyboard(String languageCode);

    InlineKeyboardMarkup getMessageKeyboard(Long messageId, String languageCode);

    InlineKeyboardMarkup getViewKeyboard(
            Long messageId, String languageCode, boolean isForwardedMessage, boolean isSimpleMessage);

    InlineKeyboardMarkup getDeleteKeyboard(Long messageId, String languageCode);

    InlineKeyboardMarkup getRestartKeyboard(Long messageId, String languageCode);

    InlineKeyboardMarkup getProfileKeyboard(String languageCode);

    InlineKeyboardMarkup getAvailableLanguage(String languageCode);

    InlineKeyboardMarkup getSingleBackProfileKeyboard(String languageCode);

    InlineKeyboardMarkup getSingleBackFullMessageKeyboard(String languageCode, Long messageId);

    InlineKeyboardMarkup getSingleBackFullMessageKeyboard(String languageCode, Long messageId, String messageText);

    InlineKeyboardMarkup getQuizQuestionKeyboard(QuizQuestion qq, Long messageId, String languageCode);

    InlineKeyboardMarkup getIncorrectQuizKeyboard(String languageCode, Long messageId, Long quizId);

    ArrayList<InlineKeyboardButton> getNavigationButtons(
            int page, int size, Page<?> messages, String operation, InputUserData inputUserData);
}

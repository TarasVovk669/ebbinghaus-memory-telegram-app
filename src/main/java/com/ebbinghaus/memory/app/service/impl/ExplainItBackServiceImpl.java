package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.domain.explain.ExplainItBackEntry;
import com.ebbinghaus.memory.app.model.ExplainItBackCount;
import com.ebbinghaus.memory.app.model.ExplainItBackFeedback;
import com.ebbinghaus.memory.app.repository.ExplainItBackRepository;
import com.ebbinghaus.memory.app.service.ExplainItBackService;
import com.ebbinghaus.memory.app.service.MessageService;
import com.ebbinghaus.memory.app.service.MessageSourceService;
import com.ebbinghaus.memory.app.service.TelegramClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.ebbinghaus.memory.app.utils.Constants.DEFAULT_EXPLAIN_IT_BACK_QTY;
import static java.time.ZoneOffset.UTC;

@Service
@RequiredArgsConstructor
public class ExplainItBackServiceImpl implements ExplainItBackService {

  private static final Logger log = LoggerFactory.getLogger(ExplainItBackServiceImpl.class);

  private final TelegramClientService telegramClientService;
  private final MessageService messageService;
  private final MessageSourceService messageSourceService;
  private final OpenAiAudioTranscriptionModel transcriptionModel;
  private final OpenAiChatModel openAiChatModel;
  private final ObjectMapper objectMapper;
  private final ExplainItBackRepository explainItBackRepository;

  @Value("classpath:templates/explain-prompt.st")
  private Resource explainPrompt;

  @Override
  public boolean canExplain(Long userId) {
    var since = LocalDateTime.now(UTC).minusHours(24);
    return explainItBackRepository.countRecent(userId, since) < DEFAULT_EXPLAIN_IT_BACK_QTY;
  }

  @Override
  public ExplainItBackCount count(Long userId) {
    var since = LocalDateTime.now(UTC).minusHours(24);
    var usedCount = explainItBackRepository.countRecent(userId, since);
    return new ExplainItBackCount(DEFAULT_EXPLAIN_IT_BACK_QTY - usedCount, DEFAULT_EXPLAIN_IT_BACK_QTY);
  }

  @Override
  public void processExplain(
      Long userId,
      Long chatId,
      Long messageId,
      String voiceFileId,
      String languageCode,
      Integer receiptMessageId,
      Integer replyMessageId) {
    var message = messageService.getMessage(messageId, true);
    if (message == null || message.getText() == null || message.getText().isBlank()) {
      telegramClientService.sendMessage(
          chatId,
          messageSourceService.getMessage("messages.explain.error.no-text", languageCode));
      deleteReceiptMessage(chatId, receiptMessageId);
      return;
    }

    String transcript;
    try {
      var audioBytes = telegramClientService.downloadFile(voiceFileId);
      var audioResource = new ByteArrayResource(audioBytes) {
        @Override
        public String getFilename() {
          return "voice.ogg";
        }
      };
      transcript = transcriptionModel.call(audioResource);
    } catch (Exception e) {
      log.error("Explain-it-back transcription failed", e);
      telegramClientService.sendMessage(
          chatId,
          messageSourceService.getMessage("messages.explain.error.transcription", languageCode));
      deleteReceiptMessage(chatId, receiptMessageId);
      return;
    }

    if (transcript == null || transcript.isBlank()) {
      telegramClientService.sendMessage(
          chatId,
          messageSourceService.getMessage("messages.explain.error.transcription", languageCode));
      deleteReceiptMessage(chatId, receiptMessageId);
      return;
    }

    ExplainItBackFeedback feedback;
    try {
      var template = new PromptTemplate(explainPrompt);
      var prompt =
          template.create(
              Map.of(
                  "reference_text", message.getText(),
                  "transcript", transcript,
                  "language_code", languageCode));
      var raw = openAiChatModel.call(prompt).getResult().getOutput().getText();
      feedback = objectMapper.readValue(raw, ExplainItBackFeedback.class);
    } catch (Exception e) {
      log.error("Explain-it-back feedback generation failed", e);
      telegramClientService.sendMessage(
          chatId,
          messageSourceService.getMessage("messages.explain.error.processing", languageCode));
      deleteReceiptMessage(chatId, receiptMessageId);
      return;
    }

    var response = formatFeedback(feedback, languageCode);
    telegramClientService.sendMessage(
        chatId, response, null, null, replyMessageId != null ? replyMessageId.longValue() : null);
    deleteReceiptMessage(chatId, receiptMessageId);
    explainItBackRepository.save(
        ExplainItBackEntry.builder()
            .userId(userId)
            .messageId(messageId)
            .score(feedback.getScore())
            .summary(feedback.getSummary())
            .missingPoints(joinLines(feedback.getMissingPoints()))
            .improvements(joinLines(feedback.getImprovements()))
            .feedbackText(response)
            .createdAt(LocalDateTime.now(UTC))
            .build());
  }

  private String formatFeedback(ExplainItBackFeedback feedback, String languageCode) {
    var score = feedback.getScore() == null ? "-" : feedback.getScore().toString();
    var summary = feedback.getSummary() == null || feedback.getSummary().isBlank()
        ? messageSourceService.getMessage("messages.explain.none", languageCode)
        : feedback.getSummary();
    var missing = formatList(feedback.getMissingPoints(), languageCode);
    var improvements = formatList(feedback.getImprovements(), languageCode);

    return String.format(
        messageSourceService.getMessage("messages.explain.result", languageCode),
        score,
        summary,
        missing,
        improvements);
  }

  private String formatList(List<String> items, String languageCode) {
    if (items == null || items.isEmpty()) {
      return messageSourceService.getMessage("messages.explain.none", languageCode);
    }
    return String.join("\n", items.stream().map(i -> "- " + i).toList());
  }

  private String joinLines(List<String> items) {
    if (items == null || items.isEmpty()) {
      return null;
    }
    return String.join("\n", items);
  }

  private void deleteReceiptMessage(Long chatId, Integer receiptMessageId) {
    if (receiptMessageId == null) {
      return;
    }
    try {
      telegramClientService.deleteMessage(chatId, receiptMessageId);
    } catch (Exception e) {
      log.warn("Failed to delete explain-it-back receipt message: {}", receiptMessageId, e);
    }
  }
}

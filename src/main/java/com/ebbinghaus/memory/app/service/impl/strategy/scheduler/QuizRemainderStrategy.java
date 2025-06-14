package com.ebbinghaus.memory.app.service.impl.strategy.scheduler;

import com.ebbinghaus.memory.app.domain.EMessageEntity;
import com.ebbinghaus.memory.app.domain.ScheduleMessageErrorQueue;
import com.ebbinghaus.memory.app.exception.TelegramCallException;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.MessageUtils.*;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static java.time.ZoneOffset.UTC;

@Service
@RequiredArgsConstructor
public class QuizRemainderStrategy implements SchedulerStrategy {

    private static final Logger log = LoggerFactory.getLogger(MessageSchedulerStrategy.class);

    private final Scheduler scheduler;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final UtilityService utilityService;
    private final KeyboardService keyboardService;
    private final MessageSourceService messageSourceService;
    private final TelegramClientService telegramClientService;
    private final ScheduleMessageErrorQueueService scheduleMessageErrorQueueService;

    @Value("${app.max.try-fibonacci-time}")
    private Integer maxTryFibonacciTime;

    @Override
    public void process(JobExecutionContext context, JobDataMap jobDataMap) {
        var chatId = Long.valueOf(jobDataMap.getString("chat_id"));
        var user = userService.getUser(chatId);
        var languageCode = user.getLanguageCode();
        var isQuizRemainderEnabled = user.getQuizRemainderEnabled();

        if (isQuizRemainderEnabled) {
            var messages = messageService.selectTopMessagesForUser(chatId);
            var quizRemainderTitle = messageSourceService.getMessage("messages.quiz.remainder.title", languageCode);

            messages.stream()
                    .findFirst()
                    .ifPresentOrElse(message -> {
                        var messageString =
                                quizRemainderTitle
                                        .concat(parseMessage(message, false, false, languageCode, messageSourceService));
                        try {
                            var sentMessage =
                                    telegramClientService.sendMessage(
                                            manageMsgType(message),
                                            MessageDataRequest.builder()
                                                    .chatId(chatId)
                                                    .messageText(messageString)
                                                    .messageId(message.getId().intValue())
                                                    .entities(
                                                            manageMessageEntitiesShortMessage(
                                                                    message.getMessageEntities(),
                                                                    messageString,
                                                                    QUIZ_SHORT_MESSAGE_SYMBOL_QUANTITY,
                                                                    null,
                                                                    objectMapper,
                                                                    false,
                                                                    quizRemainderTitle.length()
                                                            ))
                                                    .replyKeyboard(
                                                            keyboardService.getQuizRemainderKeyboard(message.getId(), languageCode))
                                                    .file(message.getFile())
                                                    .build());
                            log.info("Sent message: {}", sentMessage.getMessageId());
                            utilityService.removeSchedulerTrigger(context.getJobDetail().getKey().getName());

                        } catch (TelegramCallException e) {
                            log.info("Error tg call", e);

                            if (SERVER_MOST_POPULAR_ERRORS.stream().anyMatch(error -> e.getMessage().contains(error))) {
                                var fibFirst = jobDataMap.getIntegerFromString(FIB_STEP_FIRST);
                                var fibSecond = jobDataMap.getIntegerFromString(FIB_STEP_SECOND);
                                var sum = fibFirst + fibSecond;

                                if (sum <= maxTryFibonacciTime) {
                                    log.warn(
                                            "Postpone the job with id: {}, because of error with num: {}",
                                            context.getJobDetail().getKey(),
                                            sum);

                                    jobDataMap.putAsString(FIB_STEP_FIRST, fibSecond);
                                    jobDataMap.putAsString(FIB_STEP_SECOND, sum);

                                    var nextTryExecutionTime = LocalDateTime.now(UTC).plusMinutes(sum);
                                    rescheduleJob(context, nextTryExecutionTime.toInstant(UTC));
                                } else {
                                    log.error(
                                            "Error to reschedule message with limit try_count. message_id: {}, chat_id: {}",
                                            message.getId(),
                                            chatId);

                                    scheduleMessageErrorQueueService.save(
                                            ScheduleMessageErrorQueue.builder()
                                                    .messageId(message.getId())
                                                    .chatId(chatId)
                                                    .ownerId(message.getOwnerId())
                                                    .errorText("Quiz remainder:" + e.getMessage())
                                                    .time(LocalDateTime.now(UTC))
                                                    .build());

                                    utilityService.removeSchedulerTrigger(context.getJobDetail().getKey().getName());
                                }
                            } else {
                                scheduleMessageErrorQueueService.save(
                                        ScheduleMessageErrorQueue.builder()
                                                .messageId(message.getId())
                                                .chatId(chatId)
                                                .ownerId(message.getOwnerId())
                                                .errorText("Quiz remainder:" + e.getMessage())
                                                .time(LocalDateTime.now(UTC))
                                                .build());

                                utilityService.removeSchedulerTrigger(context.getJobDetail().getKey().getName());
                            }
                        }
                    }, () -> {
                        log.info("No messages found for user with chat_id: {}", chatId);
                        utilityService.removeSchedulerTrigger(context.getJobDetail().getKey().getName());
                    });
        } else {
            log.info("Quiz remainder is disabled for user with chat_id: {}", chatId);
            utilityService.removeSchedulerTrigger(context.getJobDetail().getKey().getName());
        }
    }

    private void rescheduleJob(JobExecutionContext context, Instant date) {
        var existingJobDetail = context.getJobDetail();
        var newJobDetail =
                JobBuilder.newJob(existingJobDetail.getJobClass())
                        .withIdentity(existingJobDetail.getKey())
                        .usingJobData(existingJobDetail.getJobDataMap()) // Use the updated JobDataMap
                        .storeDurably()
                        .build();
        var newTrigger =
                TriggerBuilder.newTrigger()
                        .forJob(newJobDetail)
                        .withIdentity(existingJobDetail.getKey().getName(), "message-triggers")
                        .withDescription(
                                String.format("Send Message Trigger: %s", existingJobDetail.getKey().getName()))
                        .startAt(Date.from(date))
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                        .build();

        doTry(
                () -> {
                    scheduler.addJob(newJobDetail, true);
                    scheduler.rescheduleJob(newTrigger.getKey(), newTrigger);
                });
    }

    private static List<MessageEntity> shiftEntities(
            Collection<EMessageEntity> src,
            int shift,
            ObjectMapper mapper) {

        return src.stream()
                .map(e -> doTry(() ->
                        mapper.readValue(e.getValue(), MessageEntity.class)))
                .peek(me -> me.setOffset(me.getOffset() + shift))
                .toList();
    }
}

package com.ebbinghaus.memory.app.service.impl.strategy.scheduler;

import com.ebbinghaus.memory.app.domain.ScheduleMessageErrorQueue;
import com.ebbinghaus.memory.app.exception.TelegramCallException;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.model.ScheduleResultTuple;
import com.ebbinghaus.memory.app.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static com.ebbinghaus.memory.app.utils.Constants.*;
import static com.ebbinghaus.memory.app.utils.MessageUtils.*;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static com.ebbinghaus.memory.app.utils.SchedulerUtils.getScheduleResultTuple;
import static com.ebbinghaus.memory.app.utils.SchedulerUtils.initRetryCounts;
import static java.time.ZoneOffset.UTC;

@Service
@RequiredArgsConstructor
public class MessageSchedulerStrategy implements SchedulerStrategy {

    private static final Logger log = LoggerFactory.getLogger(MessageSchedulerStrategy.class);

    private final Scheduler scheduler;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final KeyboardService keyboardService;
    private final MessageSourceService messageSourceService;
    private final TelegramClientService telegramClientService;
    private final ScheduleMessageErrorQueueService scheduleMessageErrorQueueService;

    @Value("${app.max.try-fibonacci-time}")
    private Integer maxTryFibonacciTime;

    @Override
    public void process(JobExecutionContext context, JobDataMap jobDataMap) {
        var message =
                messageService.getUpdatedMessage(Long.valueOf(jobDataMap.getString("message_id")), true);
        var chatId = Long.valueOf(jobDataMap.getString("chat_id"));
        var languageCode = userService.getUser(message.getOwnerId()).getLanguageCode();
        var suffix = messageSourceService.getMessage("messages.suffix.execution-time", languageCode);
        var messageString = parseMessage(message, false, suffix, languageCode, messageSourceService);

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
                                                    SHORT_MESSAGE_SYMBOL_QUANTITY,
                                                    suffix,
                                                    objectMapper))
                                    .replyKeyboard(
                                            keyboardService.getMessageKeyboard(message.getId(), languageCode))
                                    .file(message.getFile())
                                    .build());
            log.info("Sent message: {}", sentMessage.getMessageId());

            ScheduleResultTuple result = getScheduleResultTuple(message, chatId, jobDataMap);

            if (doTry(() -> scheduler.checkExists(context.getJobDetail().getKey()))) {
                initRetryCounts(jobDataMap);
                rescheduleJob(context, message.getNextExecutionDateTime().atZone(UTC).toInstant());
            } else {
                initRetryCounts(jobDataMap);
                doTry(() -> scheduler.scheduleJob(result.jobDetail(), result.trigger()));
            }
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
                    var updatedMessage =
                            messageService.getUpdatedMessage(
                                    Long.valueOf(jobDataMap.getString("message_id")),
                                    message.getExecutionStep() - 1,
                                    nextTryExecutionTime);
                    log.info("Rollback message: {}", updatedMessage);
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
                                    .errorText(e.getMessage())
                                    .time(LocalDateTime.now(UTC))
                                    .build());
                }
            } else {
                scheduleMessageErrorQueueService.save(
                        ScheduleMessageErrorQueue.builder()
                                .messageId(message.getId())
                                .chatId(chatId)
                                .ownerId(message.getOwnerId())
                                .errorText(e.getMessage())
                                .time(LocalDateTime.now(UTC))
                                .build());
            }
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
}

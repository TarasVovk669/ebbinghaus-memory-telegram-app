package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.bot.KeyboardFactoryService;
import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.domain.EMessageType;
import com.ebbinghaus.memory.app.domain.ScheduleMessageErrorQueue;
import com.ebbinghaus.memory.app.exception.TelegramCallException;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.model.ScheduleResultTuple;
import com.ebbinghaus.memory.app.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static com.ebbinghaus.memory.app.bot.MemoryBot.manageMsgType;
import static com.ebbinghaus.memory.app.bot.MemoryBot.parseMessage;
import static com.ebbinghaus.memory.app.utils.Constants.SERVER_MOST_POPULAR_ERRORS;
import static com.ebbinghaus.memory.app.utils.Constants.SHORT_MESSAGE_SYMBOL_QUANTITY;
import static com.ebbinghaus.memory.app.utils.MessageUtil.manageMessageEntitiesShortMessage;
import static com.ebbinghaus.memory.app.utils.ObjectUtils.doTry;
import static java.time.ZoneOffset.UTC;

@Component
@RequiredArgsConstructor
public class SchedulerServiceImpl extends QuartzJobBean implements SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerServiceImpl.class);
    public static final String FIB_STEP_FIRST = "fib_step_first";
    public static final String FIB_STEP_SECOND = "fib_step_second";
    public static final String TRIGGERS_GROUP = "message-triggers";
    public static final String JOBS_GROUP = "message-jobs";

    private final Scheduler scheduler;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final TelegramClient telegramClient;
    private final MessageService messageService;
    private final MessageSourceService messageSourceService;
    private final KeyboardFactoryService keyboardFactoryService;
    private final ScheduleMessageErrorQueueService scheduleMessageErrorQueueService;

    @Value("${app.max.try-fibonacci-time}")
    private Integer maxTryFibonacciTime;

    @Override
    @Transactional
    protected void executeInternal(JobExecutionContext context) {
        log.info("Executing job with key: {}", context.getJobDetail().getKey());

        var jobDataMap = context.getJobDetail().getJobDataMap();

        var message = messageService.getUpdatedMessage(Long.valueOf(jobDataMap.getString("message_id")), true);
        var chatId = Long.valueOf(jobDataMap.getString("chat_id"));
        var languageCode = userService.getUser(message.getOwnerId()).getLanguageCode();
        var suffix = messageSourceService.getMessage("messages.suffix.execution-time",
                languageCode);
        var messageString = parseMessage(
                message,
                false,
                suffix,
                languageCode,
                messageSourceService);

        try {
            manageMsgType(message)
                    .sendMessage(MessageDataRequest.builder()
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
                                    .replyKeyboard(keyboardFactoryService.getMessageKeyboard(
                                            message.getId(),
                                            languageCode,
                                            message.getType().equals(EMessageType.FORWARDED)))
                                    .file(message.getFile())
                                    .build(),
                            telegramClient);

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
                    log.warn("Postpone the job with id: {}, because of error with num: {}", context.getJobDetail().getKey(), sum);

                    jobDataMap.putAsString(FIB_STEP_FIRST, fibSecond);
                    jobDataMap.putAsString(FIB_STEP_SECOND, sum);

                    var nextTryExecutionTime = LocalDateTime.now(UTC).plusMinutes(sum);
                    rescheduleJob(context, nextTryExecutionTime.toInstant(UTC));
                    var updatedMessage = messageService.getUpdatedMessage(
                            Long.valueOf(jobDataMap.getString("message_id")),
                            message.getExecutionStep() - 1,
                            nextTryExecutionTime);
                    log.info("Rollback message: {}", updatedMessage);
                } else {
                    log.error("Error to reschedule message with limit try_count. message_id: {}, chat_id: {}",
                            message.getId(),
                            chatId);

                    scheduleMessageErrorQueueService.save(ScheduleMessageErrorQueue.builder()
                            .messageId(message.getId())
                            .chatId(chatId)
                            .ownerId(message.getOwnerId())
                            .errorText(e.getMessage())
                            .time(LocalDateTime.now(UTC))
                            .build());
                }
            } else {
                scheduleMessageErrorQueueService.save(ScheduleMessageErrorQueue.builder()
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
        var newJobDetail = JobBuilder.newJob(existingJobDetail.getJobClass())
                .withIdentity(existingJobDetail.getKey())
                .usingJobData(existingJobDetail.getJobDataMap()) // Use the updated JobDataMap
                .storeDurably()
                .build();
        var newTrigger = TriggerBuilder.newTrigger()
                .forJob(newJobDetail)
                .withIdentity(existingJobDetail.getKey().getName(), "message-triggers")
                .withDescription(String.format("Send Message Trigger: %s", existingJobDetail.getKey().getName()))
                .startAt(Date.from(date))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();

        doTry(() -> {
            scheduler.addJob(newJobDetail, true);
            scheduler.rescheduleJob(newTrigger.getKey(), newTrigger);
        });
    }


    @Override
    public void scheduleMessage(EMessage message, InputUserData userData) {
        log.info("Schedule message with id: {} and chat_id:{}", message.getId(), userData.getChatId());

        var jobDataMap = new JobDataMap();
        jobDataMap.putAsString("message_id", message.getId());
        jobDataMap.putAsString("chat_id", userData.getChatId());
        initRetryCounts(jobDataMap);

        try {
            ScheduleResultTuple result = getScheduleResultTuple(message, userData.getChatId(), jobDataMap);
            scheduler.scheduleJob(result.jobDetail(), result.trigger());

            log.info("Create trigger for message with id: {} and for chat_id: {}", message.getId(), userData.getChatId());
        } catch (SchedulerException e) {
            log.error("Error scheduling message", e);
            throw new RuntimeException(e);
        }
    }

    private static void initRetryCounts(JobDataMap jobDataMap) {
        jobDataMap.putAsString(FIB_STEP_FIRST, 0);
        jobDataMap.putAsString(FIB_STEP_SECOND, 1);
    }

    @NotNull
    private ScheduleResultTuple getScheduleResultTuple(EMessage message, Long chatId, JobDataMap jobDataMap) {
        JobDetail jobDetail = JobBuilder.newJob(this.getClass())
                .withIdentity(message.getId().toString().concat(chatId.toString()), JOBS_GROUP)
                .withDescription(String.format("Send scheduled message: %d", message.getId()))
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGERS_GROUP)
                .withDescription(String.format("Send Message Trigger: %s", jobDetail.getKey().getName()))
                .startAt(Date.from(message.getNextExecutionDateTime().atZone(UTC).toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
        return new ScheduleResultTuple(jobDetail, trigger);
    }

}

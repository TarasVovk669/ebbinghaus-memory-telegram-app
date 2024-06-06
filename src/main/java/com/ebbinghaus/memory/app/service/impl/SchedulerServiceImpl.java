package com.ebbinghaus.memory.app.service.impl;

import com.ebbinghaus.memory.app.bot.KeyboardFactoryService;
import com.ebbinghaus.memory.app.domain.EMessage;
import com.ebbinghaus.memory.app.domain.EMessageEntity;
import com.ebbinghaus.memory.app.domain.EMessageType;
import com.ebbinghaus.memory.app.model.InputUserData;
import com.ebbinghaus.memory.app.model.MessageDataRequest;
import com.ebbinghaus.memory.app.service.MessageService;
import com.ebbinghaus.memory.app.service.MessageSourceService;
import com.ebbinghaus.memory.app.service.SchedulerService;
import com.ebbinghaus.memory.app.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Date;

import static com.ebbinghaus.memory.app.bot.MemoryBot.manageMsgType;
import static com.ebbinghaus.memory.app.bot.MemoryBot.parseMessage;
import static com.ebbinghaus.memory.app.utils.MessageUtil.manageMessageEntitiesLongMessage;
import static java.time.ZoneOffset.UTC;

@Component
@RequiredArgsConstructor
public class SchedulerServiceImpl extends QuartzJobBean implements SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private final MessageService messageService;

    private final ObjectMapper objectMapper;

    private final Scheduler scheduler;

    private final TelegramClient telegramClient;

    private final MessageSourceService messageSourceService;

    private final UserService userService;

    private final KeyboardFactoryService keyboardFactoryService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("Executing job with key: {}", context.getJobDetail().getKey());

        var jobDataMap = context.getMergedJobDataMap();

        var message = messageService.getUpdatedMessage(Long.valueOf(jobDataMap.getString("message_id")), true);
        var languageCode = userService.getUser(message.getOwnerId()).getLanguageCode();
        var suffix = messageSourceService.getMessage("messages.suffix.execution-time",
                languageCode);
        var messageString = parseMessage(
                message,
                false,
                suffix,
                languageCode,
                messageSourceService);

        manageMsgType(message)
                .sendMessage(MessageDataRequest.builder()
                                .chatId(Long.valueOf(jobDataMap.getString("chat_id")))
                                .messageText(messageString)
                                .messageId(message.getId().intValue())
                                .entities(
                                        manageMessageEntitiesLongMessage(
                                                message.getMessageEntities()
                                                        .stream()
                                                        .map(EMessageEntity::getValue)
                                                        .toList(),
                                                messageString, true, suffix, objectMapper))
                                .replyKeyboard(keyboardFactoryService.getMessageKeyboard(
                                        message.getId(),
                                        languageCode,
                                        message.getType().equals(EMessageType.FORWARDED)))
                                .file(message.getFile())
                                .build(),
                        telegramClient);

        try {
            scheduler.deleteJob(context.getJobDetail().getKey());
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void scheduleMessage(EMessage message, InputUserData userData) {
        var jobDataMap = new JobDataMap();
        jobDataMap.putAsString("message_id", message.getId());
        jobDataMap.putAsString("chat_id", userData.getChatId());

        try {
            JobDetail jobDetail = JobBuilder.newJob(this.getClass())
                    .withIdentity(message.getId().toString().concat(userData.getChatId().toString()), "message-jobs")
                    .withDescription(String.format("Send scheduled message: %d", message.getId()))
                    .usingJobData(jobDataMap)
                    .storeDurably()
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity(jobDetail.getKey().getName(), "message-triggers")
                    .withDescription(String.format("Send Message Trigger: %s", jobDetail.getKey().getName()))
                    .startAt(Date.from(message.getNextExecutionDateTime().atZone(UTC).toInstant()))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                    .build();


            scheduler.scheduleJob(jobDetail, trigger);

            log.info("Create trigger for message with id: {} and for chat_id: {}", message.getId(), userData.getChatId());
        } catch (SchedulerException e) {
            log.error("Error scheduling message", e);
            throw new RuntimeException(e);
        }
    }


}

package com.ebbinghaus.memory.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration;

@EnableCaching
@SpringBootApplication
@Import(TelegramBotStarterConfiguration.class)
@EnableAspectJAutoProxy
public class EbbinghausMemoryTelegramAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(EbbinghausMemoryTelegramAppApplication.class, args);


    }
}

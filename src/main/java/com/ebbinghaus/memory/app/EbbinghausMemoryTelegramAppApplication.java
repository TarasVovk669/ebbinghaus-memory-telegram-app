package com.ebbinghaus.memory.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration;

@EnableCaching
@EnableScheduling
@SpringBootApplication
@Import(TelegramBotStarterConfiguration.class)
public class EbbinghausMemoryTelegramAppApplication {

  public static void main(String[] args) {
    SpringApplication.run(EbbinghausMemoryTelegramAppApplication.class, args);
  }
}

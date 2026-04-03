package de.jansoh.rsistrategy.config;

import de.jansoh.rsistrategy.service.TelegramMessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
public class TelegramBotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramMessagingService telegramMessagingService) throws TelegramApiException {
        log.info("Explicitly registering TelegramMessagingService with TelegramBotsApi...");
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(telegramMessagingService);
            log.info("TelegramMessagingService successfully registered.");
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram bot: {}", e.getMessage());
            throw e;
        }
        return botsApi;
    }
}

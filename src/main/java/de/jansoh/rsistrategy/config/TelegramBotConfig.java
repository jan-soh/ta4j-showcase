package de.jansoh.rsistrategy.config;

import de.jansoh.rsistrategy.service.TelegramMessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Configuration class for setting up and managing a Telegram bot in a production environment.
 * This class uses the TelegramBotsApi to explicitly register a bot instance, which is
 * provided by the {@link TelegramMessagingService}.
 * <p>
 * The configuration is active only under the "prod" profile and ensures that the bot
 * is properly integrated with Telegram's long polling mechanism.
 * <p>
 * Main responsibilities include:
 * - Creating and configuring a {@link TelegramBotsApi} bean.
 * - Registering the Telegram bot implementation with Telegram's API.
 * - Logging the registration process for monitoring and debugging purposes.
 * <p>
 * An exception is thrown if the bot registration fails.
 */
@Slf4j
@Configuration
@Profile("prod")
public class TelegramBotConfig {

    /**
     * Creates and configures a {@link TelegramBotsApi} bean for registering a Telegram bot
     * using the provided {@link TelegramMessagingService}.
     *
     * @param telegramMessagingService the service implementation that handles Telegram bot messages and interactions.
     * @return an instance of {@link TelegramBotsApi} with the registered bot.
     * @throws TelegramApiException if an error occurs during bot registration.
     */
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

package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import de.jansoh.rsistrategy.model.TelegramChat;
import de.jansoh.rsistrategy.repository.PositionRepository;
import de.jansoh.rsistrategy.repository.TelegramChatRepository;
import de.jansoh.rsistrategy.service.strategy.StrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for handling messaging interactions with Telegram users,
 * leveraging Telegram's Long Polling API. This class extends
 * {@link TelegramLongPollingBot} and implements {@link MessageService}, providing
 * functionalities for bot registration, message handling, and message broadcasting.
 * This service is activated only in the "prod" profile and is configured using
 * properties for the bot's username and token.
 * <p>
 * Features include:
 * - Handling Telegram messages and commands, such as:
 * - /start: Registers a new user and starts the bot.
 * - /positions (/p): Retrieves and displays the user's last trading positions.
 * - /last-update (/l): Displays the last candle close dates for various assets.
 * - /stop (/x): Stops the trading strategy service.
 * - /start-strategy (/s): Starts the trading strategy service.
 * - Broadcasting messages to all connected Telegram chats.
 * - Logging interactions and errors during interactions with Telegram.
 * <p>
 * Constructor Parameters:
 * - `botUsername`: The username of the Telegram bot, retrieved from the configured
 * `bot.name` property.
 * - `botToken`: The access token of the bot, retrieved from the configured
 * `bot.token` property.
 * <p>
 * The service works in conjunction with repositories and other services:
 * - {@link PositionRepository}: Fetches trading position data for users.
 * - {@link TelegramChatRepository}: Manages Telegram chat registrations.
 * - {@link StrategyService}: Manages trading strategies, including starting and stopping.
 */
@Slf4j
@Component
@Profile("prod")
public class TelegramMessagingService extends TelegramLongPollingBot implements MessageService {

    /**
     * The username of the bot used in the Telegram messaging service.
     * This value is initialized from a configuration property typically
     * defined in an external configuration file and remains immutable once set.
     * It is used to uniquely identify the bot within the Telegram API.
     */
    private final String botUsername;

    /**
     * The authentication token used for interacting with the Telegram Bot API.
     * This token is a unique identifier that allows the bot to authenticate
     * and perform operations on Telegram's servers.
     * <p>
     * This field is initialized during the construction of the {@code TelegramMessagingService}
     * class and remains constant throughout the lifecycle of the instance.
     * <p>
     * It should be kept confidential to prevent unauthorized access to the bot.
     */
    private final String botToken;

    /**
     * Manages the persistence and retrieval of {@link Position} entities from the database.
     * Injected automatically by the Spring framework to allow seamless database interaction
     * within the {@link TelegramMessagingService}.
     * <p>
     * This repository provides methods to perform operations such as fetching recent positions,
     * retrieving positions by order ID, and other database interactions defined in the
     * {@link PositionRepository} interface.
     */
    @Autowired
    private PositionRepository positionRepository;

    /**
     * Repository for handling Telegram chat data storage and retrieval operations.
     * This field is auto-wired to manage persistence and database interactions
     * related to {@link TelegramChat} entities through the {@link TelegramChatRepository} interface.
     * It supports CRUD operations and custom database queries as defined in the repository.
     */
    @Autowired
    private TelegramChatRepository telegramChatRepository;

    /**
     * Service responsible for handling the strategies within the application.
     * This service provides business logic to manage and execute different
     * strategies as part of the system's operations.
     * <p>
     * The StrategyService bean is lazily initialized by the Spring container
     * and injected into this class. Lazy initialization ensures that the bean
     * will only be created when it is accessed for the first time, reducing
     * initial startup time and improving performance if not immediately needed.
     * <p>
     * Used by the TelegramMessagingService to facilitate messaging interactions
     * involving strategies.
     */
    @Autowired
    @Lazy
    private StrategyService strategyService;

    /**
     * Constructs a new TelegramMessagingService instance for handling Telegram bot interactions.
     * This constructor initializes the service with the specified bot username and bot token
     * and validates the provided configuration properties.
     *
     * @param botUsername the username of the Telegram bot, injected from the application properties.
     *                    If the provided value is null, empty, or starts with "${", a warning will
     *                    be logged, indicating that the username is not set correctly.
     * @param botToken    the authentication token of the Telegram bot, injected from the application properties.
     *                    If the provided value is null, empty, or starts with "${", a warning will be logged,
     *                    indicating that the token is not set correctly.
     */
    public TelegramMessagingService(
            @Value("${bot.name}") String botUsername,
            @Value("${bot.token}") String botToken) {
        super(botToken);
        if (botUsername == null || botUsername.isEmpty() || botUsername.startsWith("${")) {
            log.warn("Telegram bot username is not set correctly: {}", botUsername);
        }
        if (botToken == null || botToken.isEmpty() || botToken.startsWith("${")) {
            log.warn("Telegram bot token is not set correctly: {}", botToken);
        }
        this.botUsername = botUsername;
        this.botToken = botToken;
        log.info("TelegramMessagingService initialized with username: {}", botUsername);
    }

    /**
     * Broadcasts a message to all registered Telegram chats stored in the database.
     * This method retrieves a list of all TelegramChat entities from the repository
     * and sends the provided text message to each chat using the sendSimpleMessage method.
     *
     * @param text the content of the message to be broadcasted to all Telegram chats.
     *             This parameter represents the message text that will be sent to every chat
     *             found in the telegramChatRepository.
     */
    public void broadcast(String text) {
        log.info("Broadcasting message: {}", text);
        List<TelegramChat> chats = telegramChatRepository.findAll();
        for (TelegramChat chat : chats) {
            sendSimpleMessage(chat.getChatId(), text);
        }
    }

    private void sendSimpleMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Retrieves the authentication token of the Telegram bot.
     *
     * @return the bot token as a string, used for authenticating the bot with Telegram's API.
     */
    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Registers the Telegram bot with the Telegram server.
     * This method is invoked automatically by the framework when the bot is registered.
     * It logs a message indicating that the registration process has completed successfully.
     */
    @Override
    public void onRegister() {
        super.onRegister();
        log.info("TelegramMessagingService registered with Telegram server.");
    }

    /**
     * Retrieves the username of the Telegram bot.
     *
     * @return the bot username as a string, used to identify the bot in Telegram.
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Handles incoming updates received from the Telegram server and processes them based on the content of the messages.
     * This method is responsible for interpreting different commands and triggering appropriate actions.
     *
     * @param update the update object received from Telegram. It contains details about the sent message,
     *               including the chat ID, the message text, and metadata such as the sender's username or message ID.
     */
    @Override
    public void onUpdateReceived(Update update) {
        log.info("Update received from Telegram. Message ID: {}",
                update.hasMessage() ? update.getMessage().getMessageId() : "N/A");
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            log.info("Received message: '{}' from chat ID: {}", messageText, chatId);

            if (messageText.equals("/start")) {
                TelegramChat chat = TelegramChat.builder()
                        .chatId(chatId)
                        .username(update.getMessage().getFrom().getUserName())
                        .build();
                telegramChatRepository.save(chat);
                sendSimpleMessage(chatId, "Bot started. You will receive trading alerts here.");
                log.info("New chat registered: {} with username: {}", chatId, chat.getUsername());
            } else if (messageText.startsWith("/positions") || messageText.startsWith("/p")) {
                handlePositionsCommand(chatId, messageText);
            } else if (messageText.equals("/last-update") || messageText.equals("/l")) {
                handleLastUpdateCommand(chatId);
            } else if (messageText.equals("/stop") || messageText.equals("/x")) {
                handleStopCommand(chatId);
            } else if (messageText.equals("/start-strategy") || messageText.equals("/s")) {
                handleStartStrategyCommand(chatId);
            } else {
                sendSimpleMessage(chatId, "Sorry, I didn't understand that command.\nYou can use\n /start to start the bot,\n /positions (p) to view your positions,\n /last-update (l) to check the last candle close date,\n /stop (x) to stop the strategy, or\n /start-strategy (s) to start it again.");
            }
        } else {
            log.debug("Update does not contain a message with text.");
        }
    }

    private void handleLastUpdateCommand(Long chatId) {

        StringBuilder sb = new StringBuilder();

        strategyService.getLastCandleCloseTimes().forEach((assetTradeWindow, ts) -> {
            if (ts == 0) {
                sb.append(assetTradeWindow.getSymbol()).append("(").append(assetTradeWindow.getTimeframe().getShortcut()).append("): No candle data requested yet.").append("\n");
            } else {
                ZonedDateTime closeDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                sb.append(assetTradeWindow.getSymbol()).append("(").append(assetTradeWindow.getTimeframe().getShortcut()).append("): ").append(formatter.format(closeDate)).append("\n");
            }
        });
        sendSimpleMessage(chatId, "Last candle close dates:\n" + sb);
    }

    private void handleStopCommand(Long chatId) {
        strategyService.stopStrategy();
        sendSimpleMessage(chatId, "Strategy service has been stopped.");
    }

    private void handleStartStrategyCommand(Long chatId) {
        strategyService.startStrategy();
        sendSimpleMessage(chatId, "Strategy service has been started.");
    }

    private void handlePositionsCommand(Long chatId, String text) {
        String[] parts = text.split(" ");
        int n = 1;
        if (parts.length > 1) {
            try {
                n = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                sendSimpleMessage(chatId, "Invalid number format. Use e.g. /positions 3");
                return;
            }
        }

        if (n <= 0) {
            sendSimpleMessage(chatId, "Please provide a number greater than 0.");
            return;
        }

        List<Position> lastPositions = positionRepository.findLastNPositions(PageRequest.of(0, n));

        if (lastPositions.isEmpty()) {
            sendSimpleMessage(chatId, "Sorry, no positions available yet.");
        } else {
            String response = lastPositions.stream()
                    .map(this::formatPosition)
                    .collect(Collectors.joining("\n\n"));
            sendSimpleMessage(chatId, "Last " + lastPositions.size() + " positions:\n\n" + response);
        }
    }

    private String formatPosition(Position p) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        sb.append(p.getSide() == PositionSide.SHORT ? "🔴 " : "🟢 ");
        sb.append(String.format("Exit time: %s, PnL: %.2f\n", f.format(p.getClosedTime()), p.getRealizedProfit()));
        return sb.toString();
    }
}

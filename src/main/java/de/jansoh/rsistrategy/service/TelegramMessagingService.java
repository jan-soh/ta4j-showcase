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

@Slf4j
@Component
@Profile("prod")
public class TelegramMessagingService extends TelegramLongPollingBot implements MessageService {

    private final String botUsername;
    private final String botToken;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TelegramChatRepository telegramChatRepository;

    @Autowired
    @Lazy
    private StrategyService strategyService;

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

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onRegister() {
        super.onRegister();
        log.info("TelegramMessagingService registered with Telegram server.");
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

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
}

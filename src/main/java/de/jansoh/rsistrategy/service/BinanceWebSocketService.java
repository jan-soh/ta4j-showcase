package de.jansoh.rsistrategy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceWebSocketService {

    private static final String REAL_WS_BASE_URL = "wss://fstream.binance.com";
    private static final String DEMO_WS_BASE_URL = "wss://fstream.binancefuture.com";

    private final BinanceApiService binanceApiService;
    private final PositionService positionService;
    private final ObjectMapper objectMapper;
    private final Map<String, Consumer<String>> streamHandlers = new java.util.concurrent.ConcurrentHashMap<>();

    @Value("${binance.use-real-api}")
    private boolean isRealApi;

    private String listenKey;
    private WebSocket webSocket;
    private long lastCandleCloseTime = 0;

    @PostConstruct
    public void init() {
        startUserDataConnection();
    }

    public String getWebSocketBaseUrl() {
        return isRealApi ? REAL_WS_BASE_URL : DEMO_WS_BASE_URL;
    }

    public void startUserDataConnection() {
        listenKey = binanceApiService.startUserDataStream();
        if (listenKey == null) {
            log.error("Failed to get Binance Listen Key. User data WebSocket connection aborted.");
            return;
        }

        String baseUrl = getWebSocketBaseUrl();
        String wsUrl = baseUrl + "/ws/" + listenKey;

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new BinanceWebSocketListener("user-data"))
                .thenAccept(ws -> this.webSocket = ws);

        log.info("User data WebSocket connection initiated to: {}", wsUrl);
    }

    public void subscribeKlines(String symbol, String interval, Consumer<String> handler) {
        String streamName = String.format("%s@kline_%s", symbol.toLowerCase(), interval);
        streamHandlers.put(streamName, handler);

        String baseUrl = getWebSocketBaseUrl();
        String wsUrl = baseUrl + "/ws/market/" + streamName;

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new BinanceWebSocketListener(streamName));

        log.info("Kline WebSocket connection initiated to: {}", wsUrl);
    }

    @Scheduled(fixedDelay = 1800000) // 30 minutes
    public void keepAlive() {
        if (listenKey != null) {
            log.info("Keeping alive Binance Listen Key...");
            binanceApiService.keepAliveUserDataStream();
        } else {
            startUserDataConnection();
        }
    }

    private class BinanceWebSocketListener implements WebSocket.Listener {
        private final String streamName;
        private final StringBuilder messageBuffer = new StringBuilder();

        public BinanceWebSocketListener(String streamName) {
            this.streamName = streamName;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("Binance WebSocket opened for stream: {}", streamName);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                String fullMessage = messageBuffer.toString();
                if ("user-data".equals(streamName)) {
                    processUserDataMessage(fullMessage);
                } else {
                    Consumer<String> handler = streamHandlers.get(streamName);
                    if (handler != null) {
                        handler.accept(fullMessage);
                    }
                }
                messageBuffer.setLength(0);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("Binance WebSocket closed for stream {}: {} - {}. Attempting reconnect...", streamName, statusCode, reason);
            if ("user-data".equals(streamName)) {
                startUserDataConnection();
            } else {
                // For klines, we might want to re-subscribe
                String[] parts = streamName.split("@kline_");
                if (parts.length == 2) {
                    subscribeKlines(parts[0], parts[1], streamHandlers.get(streamName));
                }
            }
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("Binance WebSocket error for stream {}: {}", streamName, error.getMessage());
        }
    }

    private void processUserDataMessage(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = event.get("e") != null ? event.get("e").toString() : null;

            if ("ORDER_TRADE_UPDATE".equals(eventType)) {
                log.info("Received ORDER_TRADE_UPDATE via WebSocket");
                positionService.updatePositionFromOrderUpdate(event);
            }
        } catch (Exception e) {
            log.error("Error processing User Data WebSocket message: {}", e.getMessage());
        }
    }

    public long getLastCandleCloseTime() {
        return lastCandleCloseTime;
    }
}

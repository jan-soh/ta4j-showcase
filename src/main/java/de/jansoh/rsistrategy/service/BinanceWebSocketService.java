package de.jansoh.rsistrategy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceWebSocketService {

    private final BinanceApiService binanceApiService;
    private final PositionService positionService;
    private final ObjectMapper objectMapper;

    private String listenKey;
    private WebSocket webSocket;
    private static final String WS_BASE_URL = "wss://fstream.binance.com/ws/";
    private static final String DEMO_WS_BASE_URL = "wss://fstream.binancefuture.com/ws/";

    @PostConstruct
    public void init() {
        startConnection();
    }

    public void startConnection() {
        listenKey = binanceApiService.startUserDataStream();
        if (listenKey == null) {
            log.error("Failed to get Binance Listen Key. WebSocket connection aborted.");
            return;
        }

        String baseUrl = binanceApiService.getBaseUrl().contains("demo") ? DEMO_WS_BASE_URL : WS_BASE_URL;
        String wsUrl = baseUrl + listenKey;

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new BinanceWebSocketListener())
                .thenAccept(ws -> this.webSocket = ws);

        log.info("WebSocket connection initiated to: {}", wsUrl);
    }

    @Scheduled(fixedDelay = 1800000) // 30 minutes
    public void keepAlive() {
        if (listenKey != null) {
            log.info("Keeping alive Binance Listen Key...");
            binanceApiService.keepAliveUserDataStream();
        } else {
            startConnection();
        }
    }

    private class BinanceWebSocketListener implements WebSocket.Listener {
        private StringBuilder messageBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("Binance WebSocket opened.");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                processMessage(messageBuffer.toString());
                messageBuffer.setLength(0);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("Binance WebSocket closed: {} - {}. Attempting reconnect...", statusCode, reason);
            startConnection();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("Binance WebSocket error: {}", error.getMessage());
            startConnection();
        }
    }

    private void processMessage(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = event.get("e") != null ? event.get("e").toString() : null;

            if ("ORDER_TRADE_UPDATE".equals(eventType)) {
                log.info("Received ORDER_TRADE_UPDATE via WebSocket");
                positionService.updatePositionFromOrderUpdate(event);
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message: {}", e.getMessage());
        }
    }
}

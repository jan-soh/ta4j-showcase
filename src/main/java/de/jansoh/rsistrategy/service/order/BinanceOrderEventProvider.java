package de.jansoh.rsistrategy.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.service.BinanceApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Slf4j
@RequiredArgsConstructor
public class BinanceOrderEventProvider implements WebSocket.Listener, Runnable {

    private final String websocketApiUrl;
    private final BinanceApiService binanceApiService;
    private final ObjectMapper objectMapper;
    private final OrderUpdateEventMapper orderUpdateEventMapper;

    private HttpClient client;

    private String streamName;
    private final StringBuilder messageBuffer = new StringBuilder();

    private final List<OrderUpdateEventListener> listeners = new ArrayList<>();

    public void start() {

        if (null != client) {
            client.close();
        }

        init();

        String wsUrl = websocketApiUrl + "/private/ws/" + streamName;

        client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), this);
    }

    public void init() {

        streamName = binanceApiService.startUserDataStream();
        if (streamName == null) {
            log.error("----- WEB_SOCKET_ORDERS ----- failed to get Binance Listen Key. User data WebSocket connection aborted.");
        }
    }

    @Scheduled(fixedDelay = 1800000) // 30 minutes
    public void keepAlive() {
        if (streamName != null) {
            binanceApiService.keepAliveUserDataStream();
        } else {
            start();
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        log.info("----- WEB_SOCKET_ORDERS ----- orders listener started.");
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

        messageBuffer.append(data);

        if (last) {

            String fullMessage = messageBuffer.toString();
            onOrderUpdate(fullMessage);

            messageBuffer.setLength(0);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    private void onOrderUpdate(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = event.get("e") != null ? event.get("e").toString() : null;

            if ("ORDER_TRADE_UPDATE".equals(eventType)) {

                log.debug("----- WEB_SOCKET_ORDERS ----- order update event receivced:\n {}", message);

                Order order = orderUpdateEventMapper.map(event);
                OrderUpdateEvent orderUpdateEvent = OrderUpdateEventImpl.builder()
                        .order(order)
                        .build();

                notifyOrderUpdateListeners(orderUpdateEvent);
            }
        } catch (Exception e) {
            log.error("----- WEB_SOCKET_ORDERS ----- error processing order update message:\n {}", message, e);
        }
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {

        log.info("----- WEB_SOCKET_ORDERS ----- stream closed: {}, code {}, reason {}.", streamName, statusCode, reason);
        start();
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("----- WEB_SOCKET_KLINES ----- error for stream {}.", streamName, error);
    }

    public void addOrderUpdateEventListener(OrderUpdateEventListener listener) {
        listeners.add(listener);
    }

    private void notifyOrderUpdateListeners(OrderUpdateEvent event) {
        listeners.forEach(listener -> listener.onOrderUpdate(event));
    }

    @Override
    public void run() {
        start();
    }
}

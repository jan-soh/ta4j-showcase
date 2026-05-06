package de.jansoh.rsistrategy.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.service.broker.ApiConfiguration;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BinanceOrderEventProviderTest {

    @Mock
    private ApiConfiguration apiConfiguration;

    @Mock
    private BinanceApiService binanceApiService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderUpdateEventMapper orderUpdateEventMapper;

    @Mock
    private WebSocket webSocket;

    @Mock
    private OrderUpdateEventListener listener;

    private BinanceOrderEventProvider provider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        provider = new BinanceOrderEventProvider(apiConfiguration, binanceApiService, objectMapper, orderUpdateEventMapper);

        when(apiConfiguration.getWebsocketApiUrl()).thenReturn("ws://test.url");
    }

    @Test
    void init_startsUserDataStreamAndInitializesWebSocket() {
        when(binanceApiService.startUserDataStream()).thenReturn("test-listen-key");

        // Mock HttpClient and WebSocketBuilder
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder wsBuilder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(wsBuilder);
        when(wsBuilder.buildAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(webSocket));

        invokeSetField(provider, "client", httpClient);

        invokePrivateMethod(provider, "init");

        verify(binanceApiService).startUserDataStream();
        verify(wsBuilder).buildAsync(any(), eq(provider));

        String wsUrl = (String) invokeGetField(provider, "wsUrl");
        assertEquals("ws://test.url/private/ws/test-listen-key", wsUrl);
    }

    @Test
    void keepAlive_pingsUserDataStream() {
        invokeSetField(provider, "streamName", "active-stream");

        provider.keepAlive();

        verify(binanceApiService).keepAliveUserDataStream();
        verify(binanceApiService, never()).startUserDataStream();
    }

    @Test
    void keepAlive_restartsIfStreamNameNull() {
        invokeSetField(provider, "streamName", null);

        // Since start() is called, we should mock httpClient
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder wsBuilder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(wsBuilder);
        when(wsBuilder.buildAsync(any(), any())).thenReturn(new CompletableFuture<>());
        invokeSetField(provider, "client", httpClient);

        provider.keepAlive();

        verify(binanceApiService).startUserDataStream();
    }

    @Test
    void onOpen_setsAvailable() {
        provider.onOpen(webSocket);

        assertTrue((boolean) invokeGetField(provider, "available"));
        assertFalse((boolean) invokeGetField(provider, "preventRestart"));
    }

    @Test
    void onClose_restarts() {
        invokeSetField(provider, "streamName", "old-stream");

        // Mock restart behavior
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder wsBuilder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(wsBuilder);
        when(wsBuilder.buildAsync(any(), any())).thenReturn(new CompletableFuture<>());
        invokeSetField(provider, "client", httpClient);

        provider.onClose(webSocket, 1000, "normal");

        assertFalse((boolean) invokeGetField(provider, "available"));
        verify(binanceApiService).startUserDataStream();
    }

    @Test
    void onOrderUpdate_processesEventAndNotifiesListeners() throws Exception {
        String json = "{\"e\":\"ORDER_TRADE_UPDATE\",\"x\":\"NEW\"}";
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("e", "ORDER_TRADE_UPDATE");

        Order mockOrder = mock(Order.class);

        when(objectMapper.readValue(eq(json), eq(Map.class))).thenReturn(eventMap);
        when(orderUpdateEventMapper.map(eventMap)).thenReturn(mockOrder);

        provider.addOrderUpdateEventListener(listener);

        invokePrivateMethod(provider, "onOrderUpdate", json);

        ArgumentCaptor<OrderUpdateEvent> captor = ArgumentCaptor.forClass(OrderUpdateEvent.class);
        verify(listener).onOrderUpdate(captor.capture());
        assertEquals(mockOrder, captor.getValue().getOrder());
    }

    @Test
    void onText_buffersAndProcesses() throws Exception {
        String part1 = "{\"e\":\"ORDER_";
        String part2 = "TRADE_UPDATE\"}";

        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("e", "ORDER_TRADE_UPDATE");
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(eventMap);

        provider.onText(webSocket, part1, false);
        verify(objectMapper, never()).readValue(anyString(), any(Class.class));

        provider.onText(webSocket, part2, true);
        verify(objectMapper).readValue(eq(part1 + part2), eq(Map.class));
    }

    @Test
    void restart_waitsForPreventRestart() throws Exception {
        invokeSetField(provider, "preventRestart", true);

        // Mock start() requirements
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder wsBuilder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(wsBuilder);
        when(wsBuilder.buildAsync(any(), any())).thenReturn(new CompletableFuture<>());
        invokeSetField(provider, "client", httpClient);

        // Run restart in a separate thread because it sleeps
        Thread restartThread = new Thread(() -> provider.restart());
        restartThread.start();

        // Give it a moment to start sleeping
        Thread.sleep(500);

        assertTrue(restartThread.isAlive());

        // Clear flag
        invokeSetField(provider, "preventRestart", false);

        restartThread.join(2000);
        assertFalse(restartThread.isAlive());
        verify(binanceApiService, atLeastOnce()).startUserDataStream();
    }

    // Helper methods for reflection
    private void invokePrivateMethod(Object obj, String methodName, Object... args) {
        try {
            java.lang.reflect.Method method;
            if (args.length > 0) {
                Class<?>[] argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i].getClass();
                }
                method = obj.getClass().getDeclaredMethod(methodName, argTypes);
            } else {
                method = obj.getClass().getDeclaredMethod(methodName);
            }
            method.setAccessible(true);
            method.invoke(obj, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokeSetField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeGetField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

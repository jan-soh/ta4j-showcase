package de.jansoh.rsistrategy.service.kline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.BinanceKlineMessage;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BinanceKlinesProviderTest {

    @Mock
    private AssetTradeWindow tradeWindow;

    @Mock
    private BinanceApiService binanceApiService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocket webSocket;

    @Mock
    private KlinesUpdateEventListener listener;

    private BinanceKlinesProvider provider;

    private static final String SYMBOL = "BTCUSDT";
    private static final Timeframe TIMEFRAME = Timeframe.ONE_MINUTE;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(tradeWindow.getSymbol()).thenReturn(SYMBOL);
        when(tradeWindow.getTimeframe()).thenReturn(TIMEFRAME);

        provider = new BinanceKlinesProvider(tradeWindow, "ws://test.url", binanceApiService, objectMapper);
    }

    @Test
    void init_initializesSeriesAndFetchesKlines() {
        List<Object[]> mockKlines = new ArrayList<>();
        // [0] Open time, [1] Open, [2] High, [3] Low, [4] Close, [5] Volume, [6] Close time
        mockKlines.add(new Object[]{1000L, "100", "110", "90", "105", "10", 1999L});
        mockKlines.add(new Object[]{2000L, "105", "115", "95", "110", "15", 2999L});

        when(binanceApiService.getKlines(eq(SYMBOL), eq(TIMEFRAME.getShortcut()), anyInt())).thenReturn(mockKlines);

        // init is private, but it's called by start() or we can use reflection if start() fails due to HttpClient
        // However, start() calls init() before creating HttpClient.
        // Let's try to call start() and see if it fails on HttpClient creation in test env.
        // Actually, start() does:
        // init();
        // wsUrl = ...
        // client = HttpClient.newHttpClient();
        
        // To avoid HttpClient issues, I'll use reflection to call init() directly for this test.
        invokePrivateMethod(provider, "init");

        BarSeries series = provider.getSeries();
        assertNotNull(series);
        assertEquals(SYMBOL, series.getName());
        assertEquals(2, series.getBarCount());
        assertEquals(series.numFactory().numOf("100"), series.getBar(0).getOpenPrice());
        assertEquals(series.numFactory().numOf("105"), series.getBar(1).getOpenPrice());
        assertEquals(series.numFactory().numOf("110"), series.getBar(1).getClosePrice());
    }

    @Test
    void onKlineUpdate_addsNewBar() throws Exception {
        // Setup initial series
        List<Object[]> mockKlines = new ArrayList<>();
        mockKlines.add(new Object[]{1000L, "100", "110", "90", "105", "10", 1999L});
        when(binanceApiService.getKlines(anyString(), anyString(), anyInt())).thenReturn(mockKlines);
        invokePrivateMethod(provider, "init");

        // Mock message
        String json = "{\"e\":\"kline\",\"E\":123456789,\"s\":\"BTCUSDT\",\"k\":{}}";
        BinanceKlineMessage message = new BinanceKlineMessage();
        BinanceKlineMessage.KlineData data = new BinanceKlineMessage.KlineData();
        data.setStartTime(2000L);
        data.setCloseTime(2999L);
        data.setOpenPrice("105");
        data.setHighPrice("115");
        data.setLowPrice("95");
        data.setClosePrice("110");
        data.setVolume("15");
        data.setIsClosed(true);
        message.setKline(data);

        when(objectMapper.readValue(eq(json), eq(BinanceKlineMessage.class))).thenReturn(message);
        provider.addKlineUpdateEventListener(listener);

        // Act
        invokePrivateMethod(provider, "onKlineUpdate", json);

        // Assert
        BarSeries series = provider.getSeries();
        assertEquals(2, series.getBarCount());
        assertEquals(series.numFactory().numOf("110"), series.getLastBar().getClosePrice());

        verify(listener).onKlinesUpdate(any(KlinesUpdateEvent.class));
    }

    @Test
    void onKlineUpdate_replacesExistingBarOnFirstUpdate() throws Exception {
        // Setup initial series
        List<Object[]> mockKlines = new ArrayList<>();
        mockKlines.add(new Object[]{1000L, "100", "110", "90", "105", "10", 1999L});
        when(binanceApiService.getKlines(anyString(), anyString(), anyInt())).thenReturn(mockKlines);
        invokePrivateMethod(provider, "init");

        // Mock message with same close time as last bar
        String json = "{\"k\":{}}";
        BinanceKlineMessage message = new BinanceKlineMessage();
        BinanceKlineMessage.KlineData data = new BinanceKlineMessage.KlineData();
        data.setStartTime(1000L);
        data.setCloseTime(1999L); // Same as last bar
        data.setOpenPrice("100");
        data.setHighPrice("112"); // Updated high
        data.setLowPrice("90");
        data.setClosePrice("107"); // Updated close
        data.setVolume("12");
        data.setIsClosed(true);
        message.setKline(data);

        when(objectMapper.readValue(anyString(), eq(BinanceKlineMessage.class))).thenReturn(message);

        // Act
        invokePrivateMethod(provider, "onKlineUpdate", json);

        // Assert
        BarSeries series = provider.getSeries();
        assertEquals(1, series.getBarCount());
        assertEquals(series.numFactory().numOf("107"), series.getLastBar().getClosePrice());
        assertEquals(series.numFactory().numOf("112"), series.getLastBar().getHighPrice());
    }

    @Test
    void isUpToDate_returnsCorrectStatus() throws Exception {
        // timeframe is 1 minute
        // isUpToDate formula: diff = now - lastUpdate + 1000 - timeframeMinutes * 60000;
        // returns diff < 0;
        
        // Case 1: Just updated
        invokeSetField(provider, "lastUpdate", System.currentTimeMillis());
        assertTrue(provider.isUpToDate());

        // Case 2: Updated long ago
        invokeSetField(provider, "lastUpdate", System.currentTimeMillis() - 120000); // 2 minutes ago
        assertFalse(provider.isUpToDate());
    }

    @Test
    void onText_buffersAndProcesses() throws Exception {
        String part1 = "{\"k\":";
        String part2 = "{\"s\":\"BTC\"}}";
        
        // Mock onKlineUpdate to verify it's called
        // Since we can't easily mock private method on the real object while calling onText,
        // we'll rely on side effects or verify ObjectMapper.
        
        provider.onText(webSocket, part1, false);
        verify(objectMapper, never()).readValue(anyString(), eq(BinanceKlineMessage.class));
        
        provider.onText(webSocket, part2, true);
        verify(objectMapper).readValue(eq(part1 + part2), eq(BinanceKlineMessage.class));
    }

    @Test
    void onClose_triggersStart() {
        // This is tricky because start() creates HttpClient.
        // But we can check if start() is called by verifying effects of start() if we could.
        // Actually, let's just test that it calls start() by mocking the provider if possible, 
        // but it's the object under test.
        
        // We can check if lastUpdate was reset to Long.MAX_VALUE which happens in start()
        provider.onClose(webSocket, 1000, "normal");
        
        // lastUpdate is private, check via reflection
        long lastUpdate = (long) invokeGetField(provider, "lastUpdate");
        assertEquals(Long.MAX_VALUE, lastUpdate);
    }

    @Test
    void onOpen_logsConnection() {
        provider.onOpen(webSocket);
        // Mainly verify it doesn't throw and calls super (though super is default)
    }

    @Test
    void onError_logsError() {
        provider.onError(webSocket, new RuntimeException("test error"));
        // Mainly verify it doesn't throw
    }

    @Test
    void addKlineUpdateEventListener_addsToList() {
        provider.addKlineUpdateEventListener(listener);
        List<KlinesUpdateEventListener> listeners = (List<KlinesUpdateEventListener>) invokeGetField(provider, "listeners");
        assertTrue(listeners.contains(listener));
    }

    // Helper methods for reflection
    private void invokePrivateMethod(Object obj, String methodName, Object... args) {
        try {
            java.lang.reflect.Method method;
            if (args.length > 0) {
                Class<?>[] argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i].getClass();
                    if (argTypes[i] == String.class && methodName.equals("onKlineUpdate")) {
                         // match precisely
                    }
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

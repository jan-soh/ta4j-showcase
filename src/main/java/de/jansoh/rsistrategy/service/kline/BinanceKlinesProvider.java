package de.jansoh.rsistrategy.service.kline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.BinanceKlineMessage;
import de.jansoh.rsistrategy.service.BinanceApiService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.Num;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Slf4j
@RequiredArgsConstructor
public class BinanceKlinesProvider implements WebSocket.Listener {

    private final AssetTradeWindow tradeWindow;
    private final String websocketApiUrl;
    private final BinanceApiService binanceApiService;
    private final ObjectMapper objectMapper;

    private HttpClient client;

    private String streamName;
    private final StringBuilder messageBuffer = new StringBuilder();

    private final List<KlinesUpdateEventListener> listeners = new ArrayList<>();

    @Getter
    private BarSeries series;

    private boolean firstUpdate = true;
    private long lastUpdate = 0;
    private String wsUrl;

    public void start() {

        if (null != client) {
            client.close();
        }

        lastUpdate = Long.MAX_VALUE;
        firstUpdate = true;

        init();


        wsUrl = websocketApiUrl + "/market/ws/" + streamName;

        client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), this);

        log.info("----- WEB_SOCKET_KLINES ----- HTTP client for stream {} started", wsUrl);
    }

    private void init() {

        streamName = String.format("%s@kline_%s", tradeWindow.getSymbol().toLowerCase(), tradeWindow.getTimeframe().getShortcut());
        series = new BaseBarSeriesBuilder().withName(tradeWindow.getSymbol()).build();

        List<Object[]> klines = binanceApiService.getKlines(tradeWindow.getSymbol(), tradeWindow.getTimeframe().getShortcut(), 1500);
        for (Object[] k : klines) {
            addBar(k);
        }

        log.info("----- WEB_SOCKET_KLINES ----- klines for symbol {} at timeframe {} initialized with {} bars.", tradeWindow.getSymbol(), tradeWindow.getTimeframe().getShortcut(), klines.size());
    }

    private void addBar(Object[] k) {
        // Binance Kline format:
        // [0] Open time, [1] Open, [2] High, [3] Low, [4] Close, [5] Volume, [6] Close time...
        Instant openTime = Instant.ofEpochMilli(Long.parseLong(k[0].toString()));
        Instant endTime = Instant.ofEpochMilli(Long.parseLong(k[6].toString()));
        Num open = series.numFactory().numOf(k[1].toString());
        Num high = series.numFactory().numOf(k[2].toString());
        Num low = series.numFactory().numOf(k[3].toString());
        Num close = series.numFactory().numOf(k[4].toString());
        Num volume = series.numFactory().numOf(k[5].toString());

        BaseBar bar = new BaseBar(null, openTime, endTime, open, high, low, close, volume, series.numFactory().numOf(0), 0);
        series.addBar(bar);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        log.info("----- WEB_SOCKET_KLINES ----- klines listener connected to {}.", wsUrl);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

        messageBuffer.append(data);

        if (last) {

            String fullMessage = messageBuffer.toString();
            onKlineUpdate(fullMessage);

            messageBuffer.setLength(0);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    private void onKlineUpdate(String message) {
        try {
            BinanceKlineMessage klineMessage = objectMapper.readValue(message, BinanceKlineMessage.class);
            BinanceKlineMessage.KlineData k = klineMessage.getKline();

            if (k != null && k.getIsClosed()) {

                long lastTimestamp = series.getLastBar().getEndTime().toEpochMilli();
                log.debug("----- WEB_SOCKET_KLINES ----- last kline end time: {}, current end time: {}", lastTimestamp, k.getCloseTime());
                if (firstUpdate && k.getCloseTime() <= lastTimestamp) {
                    updateAndNotifyListeners(k, true);
                    firstUpdate = false;
                }
                if (k.getCloseTime() > lastTimestamp) {
                    updateAndNotifyListeners(k, false);
                }
            }
            lastUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Error parsing kline update message: {}", message, e);
        }
    }

    private void updateAndNotifyListeners(BinanceKlineMessage.KlineData k, boolean replaceExisting) {
        addBarFromWebSocket(k, replaceExisting);
        KlinesUpdateEvent klinesUpdateEvent = KlinesUpdateEventImpl.builder()
                .symbol(tradeWindow.getSymbol())
                .timeframe(tradeWindow.getTimeframe())
                .barSeries(series)
                .build();

        notifyKlinesUpdateListenersUpdate(klinesUpdateEvent);
    }

    private void addBarFromWebSocket(BinanceKlineMessage.KlineData k, boolean replaceExisting) {

        Instant openTime = Instant.ofEpochMilli(k.getStartTime());
        Instant endTime = Instant.ofEpochMilli(k.getCloseTime());
        Num open = series.numFactory().numOf(k.getOpenPrice());
        Num high = series.numFactory().numOf(k.getHighPrice());
        Num low = series.numFactory().numOf(k.getLowPrice());
        Num close = series.numFactory().numOf(k.getClosePrice());
        Num volume = series.numFactory().numOf(k.getVolume());

        BaseBar bar = new BaseBar(null, openTime, endTime, open, high, low, close, volume, series.numFactory().numOf(0), 0);
        series.addBar(bar, replaceExisting);

        log.info("----- WEB_SOCKET_KLINES ----- new kline for symbol {} at timeframe {} at {}.", tradeWindow.getSymbol(), tradeWindow.getTimeframe().getShortcut(), endTime);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {

        log.info("----- WEB_SOCKET_KLINES ----- stream closed: {}, code {}, reason {}.", streamName, statusCode, reason);
        start();
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("----- WEB_SOCKET_KLINES ----- error for stream {}.", streamName, error);
    }

    public void addKlineUpdateEventListener(KlinesUpdateEventListener listener) {
        listeners.add(listener);
    }

    private void notifyKlinesUpdateListenersUpdate(KlinesUpdateEvent event) {
        listeners.forEach(listener -> listener.onKlinesUpdate(event));
    }

    public boolean isUpToDate() {
        long diff = System.currentTimeMillis() - lastUpdate + 1000 - tradeWindow.getTimeframe().getMinutes() * 60000L;
        return diff < 0;
    }
}

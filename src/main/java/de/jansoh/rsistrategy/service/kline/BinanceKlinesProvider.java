package de.jansoh.rsistrategy.service.kline;

import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.BinanceKlineMessage;
import de.jansoh.rsistrategy.service.BinanceApiService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Slf4j
@RequiredArgsConstructor
public class BinanceKlinesProvider implements WebSocket.Listener, Runnable {

    private final AssetTradeWindow tradeWindow;
    private final String websocketApiUrl;
    private final BinanceApiService binanceApiService;
    private final ObjectMapper objectMapper;


    private String streamName;
    private final StringBuilder messageBuffer = new StringBuilder();

    private final List<KlinesUpdateEventListener> listeners = new ArrayList<>();

    @Getter
    private BarSeries series;

    public void start() {

        init();

        String wsUrl = websocketApiUrl + "/ws/market/" + streamName;

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), this);

        log.info("----- WEB_SOCKET_KLINES ----- klines listener started for symbol {} at timeframe {}.", tradeWindow.getSymbol(), tradeWindow.getTimeframe().getShortcut());
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
        ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(k[6].toString())), ZoneId.systemDefault());
        double open = Double.parseDouble(k[1].toString());
        double high = Double.parseDouble(k[2].toString());
        double low = Double.parseDouble(k[3].toString());
        double close = Double.parseDouble(k[4].toString());
        double volume = Double.parseDouble(k[5].toString());

        series.addBar(endTime, open, high, low, close, volume);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
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

        BinanceKlineMessage klineMessage = objectMapper.readValue(message, BinanceKlineMessage.class);
        BinanceKlineMessage.KlineData k = klineMessage.getKline();

        if (k != null && k.getIsClosed()) {

            long lastTimestamp = series.getLastBar().getEndTime().toInstant().toEpochMilli();

            if (k.getCloseTime() > lastTimestamp) {

                addBarFromWebSocket(k);

                KlinesUpdateEvent klinesUpdateEvent = KlinesUpdateEventImpl.builder()
                        .symbol(tradeWindow.getSymbol())
                        .timeframe(tradeWindow.getTimeframe())
                        .barSeries(series)
                        .build();

                notifyKlinesUpdateListeners(klinesUpdateEvent);
            }
        }
    }

    private void addBarFromWebSocket(BinanceKlineMessage.KlineData k) {

        ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(k.getCloseTime()), ZoneId.systemDefault());
        double open = Double.parseDouble(k.getOpenPrice());
        double high = Double.parseDouble(k.getHighPrice());
        double low = Double.parseDouble(k.getLowPrice());
        double close = Double.parseDouble(k.getClosePrice());
        double volume = Double.parseDouble(k.getVolume());

        series.addBar(endTime, open, high, low, close, volume);

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

    private void notifyKlinesUpdateListeners(KlinesUpdateEvent event) {
        listeners.forEach(listener -> listener.onKlinesUpdate(event));
    }

    @Override
    public void run() {
        start();
    }
}

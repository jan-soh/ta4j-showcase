package de.jansoh.rsistrategy.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
public class StrategyService {

    private final BinanceApiService binanceApiService;
    private final PositionService positionService;
    private final TelegramMessagingService telegramMessagingService;
    private final BarSeries series;
    private final Strategy strategy;
    private final ATRIndicator atr;
    private final EMAIndicatorFactory emaIndicatorFactory;
    private boolean running = true;

    @Value("${strategy.symbol:BTCUSDT}")
    private String symbol;
    @Value("${strategy.interval:1m}")
    private String interval;
    @Value("${strategy.emaTriggerLength:50}")
    private int emaTriggerLength;
    @Value("${strategy.emaFilterLength:200}")
    private int emaFilterLength;
    @Value("${strategy.atrLength:14}")
    private int atrLength;
    @Value("${strategy.tpMultiplier:3.0}")
    private double tpMultiplier;
    @Value("${strategy.slMultiplier:2.0}")
    private double slMultiplier;

    public StrategyService(BinanceApiService binanceApiService,
                           PositionService positionService,
                           TelegramMessagingService telegramMessagingService,
                           BarSeries series,
                           Strategy strategy,
                           ATRIndicator atr, EMAIndicatorFactory emaIndicatorFactory) {
        this.binanceApiService = binanceApiService;
        this.positionService = positionService;
        this.telegramMessagingService = telegramMessagingService;
        this.series = series;
        this.strategy = strategy;
        this.atr = atr;
        this.emaIndicatorFactory = emaIndicatorFactory;
    }

    @PostConstruct
    public void init() {

        System.out.println("Initializing StrategyService for " + symbol + " " + interval);

        // Load initial data (max 1500 for Binance Futures)
        List<Object[]> klines = binanceApiService.getKlines(symbol, interval, 1500);
        for (Object[] k : klines) {
            addBar(k, false);
        }
        System.out.println("Loaded " + series.getBarCount() + " initial bars.");

        // Initial setup for Binance demo
        // This is not needed for demo
        // binanceApiService.setMarginType(symbol, "ISOLATED");
        binanceApiService.setLeverage(symbol, 10);
    }

    @Scheduled(fixedDelay = 60000) // Poll every minute
    public void tick() {
        if (!running) {
            log.debug("StrategyService is stopped. Skipping tick.");
            return;
        }

        // Fetch last 2 klines to get the most recent COMPLETED one
        // kline[0] is the previous one, kline[1] is the current open one
        List<Object[]> klines = binanceApiService.getKlines(symbol, interval, 2);
        if (klines.size() < 2) return;

        Object[] lastCompletedKline = klines.get(0);
        long lastTimestamp = Long.parseLong(lastCompletedKline[0].toString());
        ZonedDateTime lastBarEndTime = series.getLastBar().getEndTime();

        // If we don't have this bar yet, add it
        if (lastTimestamp > lastBarEndTime.toInstant().toEpochMilli()) {
            addBar(lastCompletedKline, true);
            checkStrategy();
        }
    }

    private void addBar(Object[] k, boolean printDebug) {
        // Binance Kline format:
        // [0] Open time, [1] Open, [2] High, [3] Low, [4] Close, [5] Volume, [6] Close time...
        long openTime = Long.parseLong(k[0].toString());
        ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(k[6].toString())), ZoneId.systemDefault());
        double open = Double.parseDouble(k[1].toString());
        double high = Double.parseDouble(k[2].toString());
        double low = Double.parseDouble(k[3].toString());
        double close = Double.parseDouble(k[4].toString());
        double volume = Double.parseDouble(k[5].toString());

        series.addBar(endTime, open, high, low, close, volume);

        if (printDebug)
            System.out.println("Added bar: " + endTime);
    }

    private void checkStrategy() {
        int endIndex = series.getEndIndex();
        if (strategy.shouldEnter(endIndex)) {
            Num closePrice = series.getBar(endIndex).getClosePrice();
            Num atrVal = atr.getValue(endIndex);

            double entryPrice = closePrice.doubleValue();
            double sl, tp;
            String type;

            // Logic from EmaCrossStrategy: 
            // Long if open < ema50 and close > ema50
            // Short if open > ema50 and close < ema50
            double open = series.getBar(endIndex).getOpenPrice().doubleValue();
            EMAIndicator ema50 = emaIndicatorFactory.createEMAIndicator(new ClosePriceIndicator(series), emaTriggerLength);
            double ema50Val = ema50.getValue(endIndex).doubleValue();

            if (open < ema50Val && entryPrice > ema50Val) {
                type = "LONG";
                tp = entryPrice + (tpMultiplier * atrVal.doubleValue());
                sl = entryPrice - (slMultiplier * atrVal.doubleValue());
            } else {
                type = "SHORT";
                tp = entryPrice - (tpMultiplier * atrVal.doubleValue());
                sl = entryPrice + (slMultiplier * atrVal.doubleValue());
            }

            System.out.println("************************************");
            System.out.println("STRATEGY SIGNAL MATCHED!");
            System.out.println("Type: " + type);
            System.out.println("Date/Time: " + series.getBar(endIndex).getEndTime());
            System.out.println("Entry Price: " + entryPrice);
            System.out.println("Stop Loss: " + sl);
            System.out.println("Take Profit: " + tp);
            System.out.println("************************************");

            // Use PositionService to place real order with TP/SL on Binance Demo
            boolean result = positionService.createPositionWithTpSl(symbol, type, "0.01", tp, sl);
            if (!result) {
                log.error("Failed create position with TP/SL.");
                String msg = String.format("Failed to create position with TP/SL.\nType: %s\nDate/Time: %s\nEntry Price: %.2f\nStop Loss: %.2f\nTake Profit: %.2f",
                        type, series.getBar(endIndex).getEndTime(), entryPrice, sl, tp);
                telegramMessagingService.broadcast(msg);
            }
        }
    }

    public void stopStrategy() {
        this.running = false;
        log.info("Strategy service stopped.");
    }

    public void startStrategy() {
        init();
        this.running = true;
        log.info("Strategy service started.");
    }

    public boolean isRunning() {
        return running;
    }
}

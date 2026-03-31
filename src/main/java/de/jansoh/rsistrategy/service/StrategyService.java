package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.strategy.EmaCrossStrategy;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.Num;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class StrategyService {

    private final BinanceApiService binanceApiService;
    private BarSeries series;
    private Strategy strategy;
    private ATRIndicator atr;

    // Configuration
    private final String symbol = "BTCUSDT";
    private final String interval = "1m";
    private final int emaTriggerLength = 50;
    private final int emaFilterLength = 200;
    private final int atrLength = 14;
    private final double tpMultiplier = 3.0;
    private final double slMultiplier = 2.0;

    public StrategyService(BinanceApiService binanceApiService) {
        this.binanceApiService = binanceApiService;
    }

    @PostConstruct
    public void init() {
        System.out.println("Initializing StrategyService for " + symbol + " " + interval);
        series = new BaseBarSeriesBuilder().withName(symbol).build();
        
        // Load initial data (max 1500 for Binance Futures)
        List<Object[]> klines = binanceApiService.getKlines(symbol, interval, 1500);
        for (Object[] k : klines) {
            addBar(k);
        }
        System.out.println("Loaded " + series.getBarCount() + " initial bars.");

        // Build strategy
        strategy = EmaCrossStrategy.buildStrategy(series, emaTriggerLength, emaFilterLength, true, true, true, true);
        atr = new ATRIndicator(series, atrLength);
    }

    @Scheduled(fixedDelay = 60000) // Poll every minute
    public void tick() {
        // Fetch last 2 klines to get the most recent COMPLETED one
        // kline[0] is the previous one, kline[1] is the current open one
        List<Object[]> klines = binanceApiService.getKlines(symbol, interval, 2);
        if (klines.size() < 2) return;

        Object[] lastCompletedKline = klines.get(0);
        long lastTimestamp = Long.parseLong(lastCompletedKline[0].toString());
        ZonedDateTime lastBarEndTime = series.getLastBar().getEndTime();
        
        // If we don't have this bar yet, add it
        if (lastTimestamp > lastBarEndTime.toInstant().toEpochMilli()) {
            addBar(lastCompletedKline);
            checkStrategy();
        }
    }

    private void addBar(Object[] k) {
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
        System.out.println("Added bar: " + endTime);
    }

    private void checkStrategy() {
        int endIndex = series.getEndIndex();
        if (strategy.shouldEnter(endIndex)) {
            Num closePrice = series.getBar(endIndex).getClosePrice();
            Num atrVal = atr.getValue(endIndex);
            
            // Determine if it's Long or Short
            // Since our strategy combined both, we need to check which one triggered.
            // Simplified: check against EMA 50 to guess, but better to check the rules.
            // For now, let's use the logic from the PineScript/Backtest
            
            double entryPrice = closePrice.doubleValue();
            double sl, tp;
            String type;

            // Logic from EmaCrossStrategy: 
            // Long if open < ema50 and close > ema50
            // Short if open > ema50 and close < ema50
            // We'll repeat the check here to distinguish for printing
            double open = series.getBar(endIndex).getOpenPrice().doubleValue();
            // We need the EMA50 value here. We can get it from the indicators in the strategy, 
            // but for simplicity and since we have the series, we can just compute it or pass it.
            // Actually, let's just use a simple check.
            
            // For now, let's just print that a signal was found and the generic SL/TP
            // To be accurate, we'd need to know if it's long or short.
            
            // Re-calculating indicators for printing
            org.ta4j.core.indicators.EMAIndicator ema50 = new org.ta4j.core.indicators.EMAIndicator(new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series), emaTriggerLength);
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
        }
    }
}

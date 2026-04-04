package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.Position;
import jakarta.annotation.PostConstruct;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StrategyService {

    private final BinanceApiService binanceApiService;
    private final PositionService positionService;
    private final PositionRepository positionRepository;
    private final TelegramMessagingService telegramMessagingService;
    private final BarSeries series;
    private final Strategy strategy;
    private final ATRIndicator atr;
    private final EMAIndicatorFactory emaIndicatorFactory;
    private final Map<ZonedDateTime, Position> positions = new LinkedHashMap<>();

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
                           PositionRepository positionRepository,
                           TelegramMessagingService telegramMessagingService,
                           BarSeries series,
                           Strategy strategy,
                           ATRIndicator atr, EMAIndicatorFactory emaIndicatorFactory) {
        this.binanceApiService = binanceApiService;
        this.positionService = positionService;
        this.positionRepository = positionRepository;
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
        // Fetch last 2 klines to get the most recent COMPLETED one
        // kline[0] is the previous one, kline[1] is the current open one
        List<Object[]> klines = binanceApiService.getKlines(symbol, interval, 2);
        if (klines.size() < 2) return;

        Object[] lastCompletedKline = klines.get(0);
        long lastTimestamp = Long.parseLong(lastCompletedKline[0].toString());
        ZonedDateTime lastBarEndTime = series.getLastBar().getEndTime();

        // Regularly check real position status on Binance
        checkRealPositions();

        // If we don't have this bar yet, add it
        if (lastTimestamp > lastBarEndTime.toInstant().toEpochMilli()) {
            addBar(lastCompletedKline, true);
            checkStrategy();
        }
    }

    private void checkRealPositions() {
        for (Position p : positions.values()) {
            if (!p.isClosed()) {
                positionService.checkPositionStatus(p, symbol);
            }
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

            // Check if there are existing open positions
            List<Position> activePositions = positionRepository.findByClosedFalse();
            if (!activePositions.isEmpty()) {
                Position active = activePositions.get(0); // For now, we only handle one active position per symbol
                if (active.getType().equalsIgnoreCase(type)) {
                    System.out.println("Strategy signal matches and " + type + " position is already open. Updating TP/SL.");
                    positionService.updatePositionTpSl(active, symbol, tp, sl);
                    return;
                } else {
                    System.out.println("Strategy signal matches " + type + " but " + active.getType() + " is open. Flipping.");
                    positionService.closePosition(active, symbol);
                    positions.remove(active.getOpenDate());
                }
            }

            System.out.println("************************************");
            System.out.println("STRATEGY SIGNAL MATCHED!");
            System.out.println("Type: " + type);
            System.out.println("Date/Time: " + series.getBar(endIndex).getEndTime());
            System.out.println("Entry Price: " + entryPrice);
            System.out.println("Stop Loss: " + sl);
            System.out.println("Take Profit: " + tp);
            System.out.println("************************************");

            Position position = Position.builder()
                    .symbol(symbol)
                    .type(type)
                    .openDate(series.getBar(endIndex).getEndTime())
                    .entryPrice(entryPrice)
                    .stopLoss(sl)
                    .takeProfit(tp)
                    .closed(false)
                    .build();

            // Use PositionService to place real order with TP/SL on Binance Demo
            Map<String, Object> orderResponse = positionService.createPositionWithTpSl(symbol, type, "0.01", tp, sl);
            if (orderResponse != null && orderResponse.containsKey("orderId")) {
                String orderId = orderResponse.get("orderId").toString();
                position.setBinanceOrderId(orderId);

                // Get real entry price and time if available in response
                if (orderResponse.containsKey("avgPrice")) {
                    position.setEntryPrice(Double.parseDouble(orderResponse.get("avgPrice").toString()));
                }
                if (orderResponse.containsKey("updateTime")) {
                    long updateTime = Long.parseLong(orderResponse.get("updateTime").toString());
                    position.setOpenDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(updateTime), ZoneId.systemDefault()));
                }

                if (orderResponse.containsKey("tpAlgoId")) {
                    position.setTpAlgoId(orderResponse.get("tpAlgoId").toString());
                }
                if (orderResponse.containsKey("slAlgoId")) {
                    position.setSlAlgoId(orderResponse.get("slAlgoId").toString());
                }

                System.out.println("Binance Order Placed: " + orderId);
            } else {
                System.out.println("Failed to place order or TP/SL order.");
            }

            position = positionRepository.save(position);
            positions.put(position.getOpenDate(), position);

            String msg = String.format("🟢 STRATEGY SIGNAL MATCHED!\nType: %s\nDate/Time: %s\nEntry Price: %.2f\nStop Loss: %.2f\nTake Profit: %.2f",
                    type, series.getBar(endIndex).getEndTime(), entryPrice, sl, tp);
            telegramMessagingService.broadcast(msg);
        }
    }
}

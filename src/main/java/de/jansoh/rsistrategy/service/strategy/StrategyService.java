package de.jansoh.rsistrategy.service.strategy;

import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.service.BinanceApiService;
import de.jansoh.rsistrategy.service.TelegramMessagingService;
import de.jansoh.rsistrategy.service.indicator.AtrIndicatorFactory;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProvider;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProviderFactory;
import de.jansoh.rsistrategy.service.kline.KlinesUpdateEvent;
import de.jansoh.rsistrategy.service.kline.KlinesUpdateEventListener;
import de.jansoh.rsistrategy.service.position.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class StrategyService implements KlinesUpdateEventListener {

    private final EMAIndicatorFactory emaIndicatorFactory;
    private final BinanceApiService binanceApiService;
    private final PositionService positionService;
    private final TelegramMessagingService telegramMessagingService;
    private final StrategyFactory strategyFactory;
    private final AtrIndicatorFactory atrIndicatorFactory;

    private final BinanceKlinesProviderFactory binanceKlinesProviderFactory;

    private boolean running = false;

    @Value("${strategy.tpMultiplier:2.2}")
    private double tpMultiplier;

    @Value("${strategy.slMultiplier:2.0}")
    private double slMultiplier;

    @Value("${trade.position.size-percentage:5}")
    private double sizePercentage;

    @Value("${trade.position.commission-asset:USDT}")
    private String commissionAsset;

    @Value("${trade.symbol.btcusdt.leverage}")
    private int btcUsdtLeverage;

    private AssetTradeWindow smallestTradeWindow;

    Set<AssetTradeWindow> tradeWindows = new HashSet<>();
    Map<AssetTradeWindow, BinanceKlinesProvider> binanceKlinesServiceMap = new ConcurrentHashMap<>();
    Map<AssetTradeWindow, Strategy> strategyMap = new ConcurrentHashMap<>();
    Map<AssetTradeWindow, ATRIndicator> atrMap = new ConcurrentHashMap<>();

    public void start() {

        if (running) {
            log.info("----- STRATEGY_SERVICE ----- strategy was started, but it is already running.");
            return;
        }

        running = true;

        positionService.init();

        smallestTradeWindow = AssetTradeWindow.builder()
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_MINUTE)
                .leverage(btcUsdtLeverage)
                .build();

        tradeWindows.add(smallestTradeWindow);

        tradeWindows.forEach(this::init);

        log.info("----- STRATEGY_SERVICE ----- strategy service was started, monitoring {} trade windows.", tradeWindows.size());
    }

    protected void init(AssetTradeWindow tradeWindow) {

        binanceApiService.setLeverage(tradeWindow.getSymbol(), tradeWindow.getLeverage());

        BinanceKlinesProvider klinesProvider = binanceKlinesProviderFactory.create(tradeWindow);
        klinesProvider.addKlineUpdateEventListener(this);
        Thread klinesProviderThread = new Thread(klinesProvider);
        klinesProviderThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        binanceKlinesServiceMap.put(tradeWindow, klinesProvider);

        Strategy st = strategyFactory.create(klinesProvider.getSeries());
        strategyMap.put(tradeWindow, st);

        ATRIndicator atr = atrIndicatorFactory.create(klinesProvider.getSeries());
        atrMap.put(tradeWindow, atr);


    }

    protected void checkStrategy(KlinesUpdateEvent klinesUpdateEvent) {

        if (!running) {
            return;
        }

        AssetTradeWindow atw = AssetTradeWindow.builder()
                .symbol(klinesUpdateEvent.getSymbol())
                .timeframe(klinesUpdateEvent.getTimeframe())
                .build();

        BarSeries series = klinesUpdateEvent.getBarSeries();
        Strategy strategy = strategyMap.get(atw);
        ATRIndicator atr = atrMap.get(atw);

        int endIndex = series.getEndIndex();
        if (strategy.shouldEnter(endIndex)) {
            Num closePrice = series.getBar(endIndex).getClosePrice();
            Num atrVal = atr.getValue(endIndex);

            double entryPrice = closePrice.doubleValue();
            double sl, tp;
            PositionSide positionSide;

            // Logic from EmaCrossStrategy: 
            // Long if open < ema50 and close > ema50
            // Short if open > ema50 and close < ema50
            double open = series.getBar(endIndex).getOpenPrice().doubleValue();
            EMAIndicator ema50 = emaIndicatorFactory.createEMAIndicator(new ClosePriceIndicator(series), 50);
            double ema50Val = ema50.getValue(endIndex).doubleValue();

            if (open < ema50Val && entryPrice > ema50Val) {
                positionSide = PositionSide.LONG;
                tp = entryPrice + (tpMultiplier * atrVal.doubleValue());
                sl = entryPrice - (slMultiplier * atrVal.doubleValue());
            } else {
                positionSide = PositionSide.SHORT;
                tp = entryPrice - (tpMultiplier * atrVal.doubleValue());
                sl = entryPrice + (slMultiplier * atrVal.doubleValue());
            }
            log.info("----- STRATEGY_SERVICE ----- strategy signal matched! Type: {}, Date/Time: {}, Entry Price: {}, ATR: {}, Stop Loss: {}, Take Profit: {}", positionSide, series.getBar(endIndex).getEndTime(), entryPrice, atrVal.doubleValue(), sl, tp);

            BigDecimal quantity = calculateQuantity(entryPrice);

            Position position = Position.builder()
                    .side(positionSide)
                    .symbol(klinesUpdateEvent.getSymbol())
                    .timeframe(klinesUpdateEvent.getTimeframe())
                    .quantity(quantity)
                    .tpAlgoPrice(new BigDecimal(tp))
                    .slAlgoPrice(new BigDecimal(sl))
                    .build();

            // Use PositionService to place real order with TP/SL on Binance Demo
            boolean result = positionService.createPositionWithTpSl(position, true);
            if (!result) {
                log.error("----- STRATEGY_SERVICE ----- failed create position with TP/SL.");

                String msg = String.format("Failed to create position with TP/SL.\nType: %s\nDate/Time: %s\nEntry Price: %.2f\nStop Loss: %.2f\nTake Profit: %.2f",
                        positionSide, series.getBar(endIndex).getEndTime(), entryPrice, sl, tp);
                telegramMessagingService.broadcast(msg);
            }
        }
    }

    public void stopStrategy() {
        this.running = false;
        log.info("----- STRATEGY_SERVICE ----- strategy was stopped.");
    }

    public void startStrategy() {
        start();
    }

    public long getLastCandleCloseTime() {
        return binanceKlinesServiceMap.get(smallestTradeWindow).getSeries().getLastBar().getEndTime().toInstant().toEpochMilli();
    }

    private BigDecimal calculateQuantity(double currentPrice) {
        List<Map<String, Object>> balances = binanceApiService.getBalance();
        if (balances == null) {
            throw new StrategyServiceIllegalStateException("Failed to fetch account balance. Calculating position size not possible.");
        }

        BigDecimal balance = BigDecimal.ZERO;
        for (Map<String, Object> b : balances) {
            if (commissionAsset.equals(b.get("asset"))) {
                balance = new BigDecimal(b.get("balance").toString());
                break;
            }
        }

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new StrategyServiceIllegalStateException("Account balance for commission asset " + commissionAsset + " is zero. No entry possible.");
        }

        //double quantity = (balance / currentPrice) * (sizePercentage / 100.0);
        balance = balance.setScale(10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(btcUsdtLeverage));

        BigDecimal quantity = balance.divide(new BigDecimal(currentPrice), 10, RoundingMode.HALF_UP);
        quantity = quantity.multiply(new BigDecimal(sizePercentage / 100.0));

        log.info("----- STRATEGY_SERVICE ----- calculated quantity: {} (Balance: {}, Price: {}, Percentage: {})", quantity.setScale(4, RoundingMode.HALF_UP), balance.setScale(4, RoundingMode.HALF_UP), currentPrice, sizePercentage);
        return quantity;
    }

    @Override
    public void onKlinesUpdate(KlinesUpdateEvent event) {
        checkStrategy(event);
    }
}

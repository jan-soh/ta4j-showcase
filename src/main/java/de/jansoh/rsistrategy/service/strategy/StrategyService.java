package de.jansoh.rsistrategy.service.strategy;

import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import de.jansoh.rsistrategy.service.MessageService;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProvider;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProviderFactory;
import de.jansoh.rsistrategy.service.kline.KlinesUpdateEvent;
import de.jansoh.rsistrategy.service.kline.KlinesUpdateEventListener;
import de.jansoh.rsistrategy.service.position.OpenPositionRegistry;
import de.jansoh.rsistrategy.service.position.PositionService;
import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.EmaCrossConfiguration;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.EmaCrossConfigurationFactory;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.FastEmaCrossingSlowEmaStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service class responsible for managing and executing trading strategies,
 * including initialization, monitoring, and responding to market events.
 * Implements the {@link KlinesUpdateEventListener} interface to react to kline updates.
 * Utilizes various supporting services and factories for strategy creation and management.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class StrategyService implements KlinesUpdateEventListener {

    /**
     * A service instance for interacting with the Binance API.
     * This variable is used to perform API requests to Binance for operations such as
     * retrieving market data, placing orders, and accessing account information.
     * The service encapsulates the logic for communicating with Binance's REST API endpoints.
     */
    private final BinanceApiService binanceApiService;

    /**
     * A service responsible for managing positional data and related operations.
     * This may include operations such as retrieval, updating, or processing
     * of positional information in the application.
     */
    private final PositionService positionService;

    /**
     * A service instance responsible for handling message-related operations such as
     * sending, receiving, or processing messages within the application.
     * This is a private and final variable, ensuring that it cannot be modified
     * after initialization and is accessible only within the enclosing class.
     */
    private final MessageService messageService;

    /**
     * A registry that tracks and manages open positions within a system.
     * This variable is used to store an instance of {@code OpenPositionRegistry},
     * which provides mechanisms to maintain and query open positions.
     * <p>
     * The {@code openPositionRegistry} is declared as {@code final}, ensuring
     * that its reference cannot be modified once initialized.
     */
    private final OpenPositionRegistry openPositionRegistry;

    /**
     * A factory instance responsible for creating and providing configurations
     * specific to the EMA (Exponential Moving Average) cross strategy.
     * This factory encapsulates the creation logic and ensures that the
     * appropriate configuration is constructed and configured.
     */
    private final EmaCrossConfigurationFactory strategyConfigurationFactory;

    /**
     * A factory instance responsible for creating strategies based on the concept of
     * a fast Exponential Moving Average (EMA) crossing a slow Exponential Moving Average.
     * This factory encapsulates the logic required to instantiate specific strategy
     * implementations tailored to this trading or analysis methodology.
     */
    private final FastEmaCrossingSlowEmaStrategyFactory strategyFactory;
    private final BinanceKlinesProviderFactory binanceKlinesProviderFactory;

    /**
     * Indicates whether the process, task, or operation is currently active or running.
     * The value is set to {@code true} when the operation is in progress
     * and {@code false} when it is not.
     */
    private boolean running = false;

    /**
     * Represents the percentage of the trade position size.
     * <p>
     * This variable is configured using an external property, allowing
     * customization through environment configuration or application
     * properties files. The default value is set to 5 if no value is
     * provided.
     * <p>
     * The value is typically used to compute or determine the portion
     * or ratio of a trade position in relation to some reference.
     */
    @Value("${trade.position.size-percentage:5}")
    private double sizePercentage;

    /**
     * Represents the asset used for commission charges in trade positions.
     * The value of this variable is configurable and can be set through the
     * 'trade.position.commission-asset' property. If not explicitly specified,
     * the default value is "USDT".
     */
    @Value("${trade.position.commission-asset:USDT}")
    private String commissionAsset;

    /**
     * A configuration property representing the strategies to be created for trading.
     * The value is injected from the application properties using the "trade.strategy.create" key.
     * Typically used to determine and configure trading strategies dynamically during runtime.
     */
    @Value("${trade.strategy.create}")
    private String strategiesToCreate;

    /**
     * Represents the smallest trade window available for asset trading.
     * This variable holds an instance of {@code AssetTradeWindow}, which defines
     * the minimum time frame or criteria required for a trade to take place.
     * It is used to determine constraints or limits on trading intervals.
     */
    private AssetTradeWindow smallestTradeWindow;

    /**
     * A set containing trade window information for various assets.
     * This collection represents the periods during which trades
     * are allowed or configured for the assets in a trading system.
     * Each trade window is represented as an instance of {@code AssetTradeWindow}.
     * The set ensures that trade windows are unique and prevents duplicates.
     * <p>
     * This variable is immutable after initialization to maintain thread safety
     * and to ensure consistency of trade window configurations.
     */
    private final Set<AssetTradeWindow> tradeWindows = new HashSet<>();

    /**
     * A thread-safe map that associates an {@code AssetTradeWindow} with a corresponding
     * {@code BinanceKlinesProvider}. This map is used to manage and retrieve the
     * Binance Klines providers for specific trading windows of assets.
     * <p>
     * The {@code ConcurrentHashMap} implementation ensures that the map operations
     * are safe for concurrent use in a multithreaded environment.
     */
    private final Map<AssetTradeWindow, BinanceKlinesProvider> binanceKlinesServiceMap = new ConcurrentHashMap<>();

    /**
     * A thread-safe map that associates each {@link AssetTradeWindow} with a corresponding
     * {@link ConditionalStrategy}. This map acts as a storage mechanism for managing and
     * retrieving strategies based on specific trading windows for assets.
     * <p>
     * The use of {@link ConcurrentHashMap} ensures that the map can be accessed and updated
     * safely across multiple threads without requiring explicit synchronization.
     */
    private final Map<AssetTradeWindow, ConditionalStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * A mapping of trade windows to their respective EMA (Exponential Moving Average)
     * cross configurations. The map is designed to associate specific trading strategies
     * with the corresponding asset trade windows.
     * <p>
     * This variable is immutable, ensuring that the mappings cannot be modified after
     * initialization, which provides thread safety and consistent behavior throughout
     * the application's lifecycle.
     * <p>
     * Each key in the map represents an {@code AssetTradeWindow}, which defines the
     * temporal context or trading interval for a specific asset. Values associated with
     * these keys are instances of {@code EmaCrossConfiguration}, which encapsulate the
     * parameters and logic for implementing the EMA cross strategy for the given trade window.
     */
    private final Map<AssetTradeWindow, EmaCrossConfiguration> strategyConfigurations = new HashMap<>();

    /**
     * Starts the strategy service. This method initializes and begins the process of monitoring
     * trade windows for the configured strategies. If the service is already running, the method
     * logs a message and exits without re-initializing.
     * <p>
     * The method performs the following tasks:
     * - Checks if the service is already running. If so, logs a message and exits.
     * - Sets the running flag to true, indicating the service is active.
     * - Initializes the position service.
     * - Parses the strategy configuration files specified in the `strategiesToCreate` field.
     * - For each configuration file, creates a corresponding strategy configuration
     * using the `strategyConfigurationFactory` and initializes the strategy.
     * - Logs the start of the strategy service along with the number of trade windows being monitored.
     */
    public void start() {

        if (running) {
            log.info("----- STRATEGY_SERVICE ----- strategy was started, but it is already running.");
            return;
        }

        running = true;

        positionService.init();

        String[] strategyConfigFiles = strategiesToCreate.split("\\s*,\\s*");
        for (String strategyConfigFile : strategyConfigFiles) {
            EmaCrossConfiguration configuration = strategyConfigurationFactory.create(strategyConfigFile);
            init(configuration);
        }

        log.info("----- STRATEGY_SERVICE ----- strategy service was started, monitoring {} trade windows.", tradeWindows.size());
    }

    protected void init(EmaCrossConfiguration configuration) {

        AssetTradeWindow tradeWindow = configuration.getAssetTradeWindow();
        strategyConfigurations.put(tradeWindow, configuration);

        if (null == smallestTradeWindow || tradeWindow.getTimeframe().getMinutes() < smallestTradeWindow.getTimeframe().getMinutes()) {
            smallestTradeWindow = tradeWindow;
        }

        tradeWindows.add(tradeWindow);

        binanceApiService.setLeverage(tradeWindow.getSymbol(), tradeWindow.getLeverage());

        BinanceKlinesProvider klinesProvider = binanceKlinesProviderFactory.create(tradeWindow);
        klinesProvider.addKlineUpdateEventListener(this);
        klinesProvider.start();

        int tries = 10;
        while (null == klinesProvider.getSeries() && tries-- > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (0 == tries) {
            throw new RuntimeException("Failed to start klines provider for " + tradeWindow);
        }

        binanceKlinesServiceMap.put(tradeWindow, klinesProvider);

        ConditionalStrategy as = strategyFactory.create(configuration, klinesProvider.getSeries());
        strategyMap.put(tradeWindow, as);
    }

    /**
     * Handles the Klines update event by invoking the appropriate strategy check
     * based on the provided event details.
     *
     * @param event the event containing Klines update data, including information
     *              necessary for evaluating and processing the update.
     */
    @Override
    public void onKlinesUpdate(KlinesUpdateEvent event) {
        checkStrategy(event);
    }

    /**
     * Evaluates the trading strategy for the provided KlinesUpdateEvent. The method performs checks for
     * both entry and exit conditions of a trading strategy, and takes appropriate actions such as
     * closing positions or creating new positions with take profit and stop loss levels.
     *
     * @param klinesUpdateEvent The event containing updated kline data, including the symbol, timeframe,
     *                          and associated bar series to evaluate strategies against.
     */
    public void checkStrategy(KlinesUpdateEvent klinesUpdateEvent) {

        if (!running) {
            return;
        }

        AssetTradeWindow atw = AssetTradeWindow.builder()
                .symbol(klinesUpdateEvent.getSymbol())
                .timeframe(klinesUpdateEvent.getTimeframe())
                .build();

        BarSeries series = klinesUpdateEvent.getBarSeries();
        ConditionalStrategy strategy = strategyMap.get(atw);

        int endIndex = series.getEndIndex();
        ZonedDateTime endDate = series.getBar(endIndex).getEndTime().atZone(ZoneId.systemDefault());
        Num closePrice = series.getBar(endIndex).getClosePrice();

        // --- Exit Check ---
        if (openPositionRegistry.hasPositions(atw)) {
            List<Position> openPositions = openPositionRegistry.getPositions(atw);
            for (Position p : openPositions) {
                if (strategy.isLongExitSatisfied(endIndex, p) && p.isLong()) {
                    log.info("----- STRATEGY_SERVICE ----- strategy exit signal matched for long position {} at {}!", p.getOrderId(), endDate);
                    p.setAverageClosedPrice(closePrice.bigDecimalValue());
                    p.setClosedTime(endDate);
                    p.setClosed(true);
                    positionService.closeMarketPosition(p);
                    continue;
                }
                if (strategy.isShortExitSatisfied(endIndex, p) && p.isShort()) {
                    log.info("----- STRATEGY_SERVICE ----- strategy exit signal matched for short position {} at {}!", p.getOrderId(), endDate);
                    positionService.closeMarketPosition(p);
                    p.setAverageClosedPrice(closePrice.bigDecimalValue());
                    p.setClosedTime(endDate);
                    p.setClosed(true);
                    continue;
                }
            }
        }

        // --- Entry Check ---
        boolean longEntry = strategy.isLongEntrySatisfied(endIndex);
        boolean shortEntry = strategy.isShortEntrySatisfied(endIndex);

        if (longEntry || shortEntry) {

            double entryPrice = closePrice.doubleValue();
            PositionSide positionSide = longEntry ? PositionSide.LONG : PositionSide.SHORT;


            BigDecimal quantity = calculateQuantity(entryPrice, strategy.getConfiguration());

            Position position = Position.builder()
                    .side(positionSide)
                    .openTime(endDate)
                    .averageOpenPrice(BigDecimal.valueOf(entryPrice))
                    .symbol(klinesUpdateEvent.getSymbol())
                    .timeframe(klinesUpdateEvent.getTimeframe())
                    .quantity(quantity)
                    .entryIndex(endIndex)
                    .build();

            position.setTpAlgoPrice(strategy.getTp(series.getBar(endIndex), position));
            position.setSlAlgoPrice(strategy.getSl(series.getBar(endIndex), position));

            log.info("----- STRATEGY_SERVICE ----- strategy signal matched! Type: {}, Date/Time: {}, Entry Price: {}, Stop Loss: {}, Take Profit: {}", positionSide, series.getBar(endIndex).getEndTime(), entryPrice, position.getSlAlgoPrice(), position.getTpAlgoPrice());

            // Use PositionService to place real order with TP/SL on Binance Demo
            boolean result = positionService.createPositionWithTpSl(position, false);
            if (!result) {
                log.error("----- STRATEGY_SERVICE ----- failed create position with TP/SL.");
            }
        }
    }

    /**
     * Stops the currently running strategy by setting the running flag to false.
     * This method is responsible for halting the operation of the strategy service
     * and logging the action for auditing or debugging purposes.
     */
    public void stopStrategy() {
        this.running = false;
        log.info("----- STRATEGY_SERVICE ----- strategy was stopped.");
    }

    /**
     * Initiates the execution of a strategy by invoking the start method.
     * This method serves as an entry point to trigger the associated
     * strategy's processing or workflow.
     */
    public void startStrategy() {
        start();
    }

    /**
     * This method is scheduled to execute periodically at a fixed rate of 60 seconds.
     * It verifies the status of Binance Klines providers for all active asset trade windows
     * in the system and takes appropriate actions if the providers are not up-to-date.
     * <p>
     * The method performs the following steps:
     * - Checks if the system is currently running.
     * - Iterates through all configured asset trade windows.
     * - Retrieves the corresponding BinanceKlinesProvider for each trade window.
     * - Skips the trade window if:
     * - The provider is null.
     * - The provider is up-to-date.
     * - Logs a message and re-initializes the provider if it is not up-to-date.
     * - Broadcasts an alert via the message service to notify users of the restart,
     * encouraging them to validate their positions.
     */
    @Scheduled(fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    private void checkKlineProviders() {
        if (running) {
            for (AssetTradeWindow atw : tradeWindows) {
                BinanceKlinesProvider klinesProvider = binanceKlinesServiceMap.get(atw);

                if (null == klinesProvider || klinesProvider.isUpToDate()) {
                    continue;
                }
                log.info("----- STRATEGY_SERVICE ----- klines provider for {} is not up to date, starting new klines provider.", atw);
                init(strategyConfigurations.get(atw));
                messageService.broadcast("/!\\ Klines provider for " + atw + " is not up to date. It was restarted, but you should validate your positions anyway.");
            }
        }
    }

    /**
     * Retrieves the closing times of the last candle (last bar) for all asset trade windows.
     * The method iterates through the collection of Binance Klines providers and maps
     * each asset trade window to the epoch millisecond timestamp of the last bar's end time.
     *
     * @return A map where the keys are {@code AssetTradeWindow} instances and the values
     * are the epoch millisecond timestamps representing the close time
     * of the last candle for the respective asset trade window.
     */
    public Map<AssetTradeWindow, Long> getLastCandleCloseTimes() {

        Map<AssetTradeWindow, Long> closeTimes = new HashMap<>();

        binanceKlinesServiceMap.forEach((assetTradeWindow, binanceKlinesProvider) ->
                closeTimes.put(assetTradeWindow, binanceKlinesProvider.getSeries().getLastBar().getEndTime().toEpochMilli()));

        return closeTimes;
    }

    private BigDecimal calculateQuantity(double currentPrice, StrategyConfiguration strategyConfiguration) {
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
        balance = balance.setScale(10, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(strategyConfiguration.getAssetTradeWindow().getLeverage()));

        BigDecimal quantity = balance.divide(new BigDecimal(currentPrice), 10, RoundingMode.HALF_UP);
        quantity = quantity.multiply(new BigDecimal(sizePercentage / 100.0));

        log.info("----- STRATEGY_SERVICE ----- calculated quantity: {} (Balance: {}, Price: {}, Percentage: {})", quantity.setScale(4, RoundingMode.HALF_UP), balance.setScale(4, RoundingMode.HALF_UP), currentPrice, sizePercentage);
        return quantity;
    }
}

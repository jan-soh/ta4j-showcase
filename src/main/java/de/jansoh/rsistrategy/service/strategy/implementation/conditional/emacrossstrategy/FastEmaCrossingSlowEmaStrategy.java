package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import de.jansoh.rsistrategy.service.strategy.StrategyConfiguration;
import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import de.jansoh.rsistrategy.ta4jbridge.model.TaPosition;
import de.jansoh.rsistrategy.ta4jbridge.model.TaTrade;
import de.jansoh.rsistrategy.ta4jbridge.model.TaTradingRecord;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Strategy implementation based on a fast EMA (Exponential Moving Average) crossing a slow EMA.
 * It provides rules for both long and short trading positions, as well as methods to calculate
 * stop-loss (SL) and take-profit (TP) levels.
 * This class is intended to be used with a given bar series and configuration for EMA crossing logic.
 */
public class FastEmaCrossingSlowEmaStrategy implements ConditionalStrategy {

    /**
     * Represents a series of bar data (e.g., candlestick data) used as the primary input for
     * calculating trading signals in the strategy. Each bar in the series corresponds to market
     * data for a specific time period and contains information such as open, high, low, close prices,
     * and trading volume.
     * <p>
     * This variable is immutable and central to the strategy's logic, serving as the data source
     * for evaluating entry and exit conditions for trades.
     */
    private final BarSeries barSeries;

    /**
     * Represents the configuration settings for the EMA (Exponential Moving Average)
     * crossover strategy used in the {@code FastEmaCrossingSlowEmaStrategy}.
     * <p>
     * The configuration encapsulates the parameters necessary to define the behavior
     * of the crossover strategy, such as the period lengths for the fast and slow EMAs,
     * and other related properties. This configuration is used to set up the strategy
     * and evaluate its trading rules and conditions.
     */
    private EmaCrossConfiguration configuration;

    /**
     * Represents the set of rules used to define conditions for entering and exiting long positions
     * in the context of the FastEmaCrossingSlowEmaStrategy. These rules are typically based on
     * exponential moving average (EMA) values and other market indicators.
     * <p>
     * This variable is utilized internally by methods that evaluate whether the criteria for long
     * position entry or exit are satisfied at a given point in the bar series.
     */
    private EmaCrossLongRules longRules;

    /**
     * Represents the set of rules for determining short entry and exit conditions
     * in the {@code FastEmaCrossingSlowEmaStrategy}. These rules are based on
     * the crossing of a fast EMA (Exponential Moving Average) with a slow EMA.
     * The {@code shortRules} field is utilized internally to evaluate whether
     * conditions for entering or exiting a short position are satisfied.
     */
    private EmaCrossShortRules shortRules;

    /**
     * Constructs a new instance of the FastEmaCrossingSlowEmaStrategy with the specified bar series.
     * This strategy uses exponential moving average (EMA) crossover rules to determine entry and exit points
     * for both long and short positions.
     *
     * @param barSeries the bar series containing market data used for strategy computation
     */
    public FastEmaCrossingSlowEmaStrategy(BarSeries barSeries) {
        this.barSeries = barSeries;
        configuration = new EmaCrossConfiguration();
        configuration.setDefaults();
        longRules = new EmaCrossLongRules(configuration, barSeries);
        shortRules = new EmaCrossShortRules(configuration, barSeries);
    }

    /**
     * Constructs a new instance of the FastEmaCrossingSlowEmaStrategy with the specified bar series and configuration.
     * This strategy uses exponential moving average (EMA) crossover rules defined in the provided configuration
     * to determine entry and exit points for both long and short positions.
     *
     * @param barSeries     the bar series containing market data used for strategy computation
     * @param configuration the configuration settings specifying the parameters for EMA crossovers and other
     *                      strategy-specific rules
     */
    public FastEmaCrossingSlowEmaStrategy(BarSeries barSeries, EmaCrossConfiguration configuration) {
        this.barSeries = barSeries;
        this.configuration = configuration;
        longRules = new EmaCrossLongRules(configuration, barSeries);
        shortRules = new EmaCrossShortRules(configuration, barSeries);
    }

    /**
     * Checks whether the conditions for a long entry trade are satisfied at the specified index.
     * This method evaluates the entry rule defined for the long strategy using the provided index.
     *
     * @param index the index of the bar in the bar series to evaluate the long entry condition for
     * @return {@code true} if the long entry condition is satisfied at the given index;
     * {@code false} otherwise
     */
    @Override
    public boolean isLongEntrySatisfied(int index) {

        TaTradingRecord tradingRecord = new TaTradingRecord(Integer.toString(index), TaTrade.TradeType.BUY);
        return longRules.getEntryRule().isSatisfied(index, tradingRecord);
    }

    /**
     * Checks whether the conditions for a long exit trade are satisfied at the specified index.
     * This method evaluates the exit rule defined for the long strategy using the provided index
     * and position.
     *
     * @param index    the index of the bar in the bar series to evaluate the long exit condition for
     * @param position the position for which the long exit condition is to be evaluated
     * @return {@code true} if the long exit condition is satisfied at the given index;
     * {@code false} otherwise
     */
    @Override
    public boolean isLongExitSatisfied(int index, Position position) {

        TaTrade entryTrade = new TaTrade(position.getEntryIndex(), barSeries, TaTrade.TradeType.BUY);
        TaPosition taPosition = new TaPosition(entryTrade);
        TaTradingRecord tradingRecord = new TaTradingRecord(taPosition);

        return longRules.getExitRule().isSatisfied(index, tradingRecord);
    }

    /**
     * Checks whether the conditions for a short entry trade are satisfied at the specified index.
     * This method evaluates the entry rule defined for the short strategy using the provided index.
     *
     * @param index the index of the bar in the bar series to evaluate the short entry condition for
     * @return {@code true} if the short entry condition is satisfied at the given index;
     * {@code false} otherwise
     */
    @Override
    public boolean isShortEntrySatisfied(int index) {

        TaTradingRecord tradingRecord = new TaTradingRecord(Integer.toString(index), TaTrade.TradeType.SELL);
        return shortRules.getEntryRule().isSatisfied(index, tradingRecord);
    }

    /**
     * Checks whether the conditions for a short exit trade are satisfied at the specified index.
     * This method evaluates the exit rule defined for the short strategy using the provided index
     * and position, leveraging the trading record constructed from the given position.
     *
     * @param index    the index of the bar in the bar series to evaluate the short exit condition for
     * @param position the position for which the short exit condition is to be evaluated
     * @return {@code true} if the short exit condition is satisfied at the given index;
     * {@code false} otherwise
     */
    @Override
    public boolean isShortExitSatisfied(int index, Position position) {

        TaTrade entryTrade = new TaTrade(position.getEntryIndex(), barSeries, TaTrade.TradeType.SELL);
        TaPosition taPosition = new TaPosition(entryTrade);
        TaTradingRecord tradingRecord = new TaTradingRecord(taPosition);

        return shortRules.getExitRule().isSatisfied(index, tradingRecord);
    }

    /**
     * Calculates the stop-loss (SL) level for the given position based on its entry data and position side.
     *
     * @param positionEntry the entry bar of the position, containing market data such as open and close prices
     * @param position      the position for which the stop-loss level is to be calculated
     * @return the calculated stop-loss level as a {@code BigDecimal}; the value is lower than the
     * open price for LONG positions and higher for non-LONG positions
     */
    @Override
    public BigDecimal getSl(Bar positionEntry, Position position) {

        Num height = positionEntry.getClosePrice()
                .minus(positionEntry.getOpenPrice())
                .multipliedBy(DecimalNum.valueOf(configuration.getSlMultiplier()))
                .abs();

        if (PositionSide.LONG == position.getSide()) {
            return new BigDecimal(positionEntry.getOpenPrice().minus(height).toString());
        }
        return new BigDecimal(positionEntry.getOpenPrice().plus(height).toString());
    }


    /**
     * Calculates the take-profit (TP) level for a given position based on its entry data and position side.
     *
     * @param positionEntry the entry bar of the position, containing market data such as open and close prices
     * @param position      the position for which the take-profit level is to be calculated
     * @return the calculated take-profit level as a {@code BigDecimal}; the value is based on specific rules
     * for LONG positions and adjusted appropriately for other positions to meet API requirements
     */
    @Override
    public BigDecimal getTp(Bar positionEntry, Position position) {
        if (PositionSide.LONG == position.getSide()) {
            return positionEntry.getOpenPrice().multipliedBy(DecimalNum.valueOf(2.0)).bigDecimalValue();
        }
        // TP of zero does not work for many APIs. 10% of the entry price should do it.
        // Plus Binance also requires orders to have a notional value of at least 50 (USDT) -> quantity * price = notational value
        BigDecimal price;
        BigDecimal bestPrice = positionEntry.getOpenPrice().multipliedBy(DecimalNum.valueOf(0.1)).bigDecimalValue();
        BigDecimal minPrice = BigDecimal.valueOf(50.000).divide(position.getQuantity(), RoundingMode.HALF_UP);
        if (bestPrice.compareTo(minPrice) < 0) {
            return minPrice;
        } else {
            return bestPrice;
        }
    }

    /**
     * Retrieves the configuration associated with the strategy.
     *
     * @return the {@code StrategyConfiguration} object representing the configuration
     * settings used by the strategy.
     */
    @Override
    public StrategyConfiguration getConfiguration() {
        return this.configuration;
    }
}

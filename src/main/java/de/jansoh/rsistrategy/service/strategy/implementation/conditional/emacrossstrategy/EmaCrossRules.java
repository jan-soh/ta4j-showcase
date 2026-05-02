package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;

import java.time.ZoneId;

/**
 * EmaCrossRules is an abstract class that contains the foundational logic
 * for implementing trading rule configurations based on Exponential
 * Moving Averages (EMAs), Relative Strength Index (RSI), Moving Average
 * Convergence Divergence (MACD), and volume filters.
 * <p>
 * This class encapsulates various indicators and custom rules for
 * analyzing a financial instrument's price bar series and determining
 * entry and exit conditions for trades.
 * <p>
 * Key Features:
 * - Calculates EMAs with configurable lengths (e.g., EMA20, EMA50, EMA200).
 * - Implements RSI and MACD indicators for momentum analysis.
 * - Provides filters based on volume and EMA distance.
 * - Allows customization of entry dates and other rule configurations.
 * - Includes utility methods such as calculating the slope/angle of EMA values.
 * <p>
 * Subclasses are responsible for defining specific entry and exit rules
 * by implementing the abstract methods `getEntryRule` and `getExitRule`.
 * <p>
 * Configuration:
 * - The class relies on an EmaCrossConfiguration object to drive
 * parameterized behavior, including indicator settings, filters, and
 * rule toggles.
 * <p>
 * Indicators and Filters:
 * - closePrice, openPrice, highPrice, lowPrice, and volume indicators.
 * - EMA20, EMA50, and EMA200 indicators for trend analysis.
 * - MACD line for convergence and divergence-based signal generation.
 * - RSI for overbought/oversold conditions.
 * - Custom implementation of distance-based filtering for EMA200 proximity.
 * - Configurable volume comparison to SMA-based average volume.
 * <p>
 * Subclasses:
 * - Potential subclasses like EmaCrossLongRules and EmaCrossShortRules
 * implement specific logic for respective trading strategies.
 * <p>
 * Methods:
 * - `getEntryRule`: Abstract method to retrieve the entry rule for trades.
 * - `getExitRule`: Abstract method to retrieve the exit rule for trades.
 * - `calculateAngle`: A helper method to calculate the gradient or slope
 * of the EMA20 indicator between consecutive indices.
 */
public abstract class EmaCrossRules {

    /**
     * Holds the configuration settings required for the EMA cross trading rules.
     * This configuration object encapsulates parameters and preferences for
     * customizing the behavior of the {@code EmaCrossRules} class.
     */
    protected final EmaCrossConfiguration configuration;

    /**
     * Represents the time series data for bar-based financial data, such as open, high, low, close prices,
     * and volume values. This variable is used for applying technical analysis indicators and rules.
     * <p>
     * This field is immutable and integral to defining trading strategies within the context of the class.
     */
    protected final BarSeries barSeries;

    /**
     * Represents the open price indicator for a financial instrument
     * within the context of EMA (Exponential Moving Average) crossover rules.
     * Tracks the open price data across a series of bars.
     */
    protected OpenPriceIndicator openPrice;

    /**
     * Represents the high price data series used in the EMA crossing strategy rules.
     * This indicator provides access to the high prices of financial instruments
     * stored within the associated BarSeries.
     */
    protected HighPriceIndicator highPrice;

    /**
     * Represents an indicator that provides the lowest price points within the context
     * of the associated bar series.
     * <p>
     * This variable is a component of the {@code EmaCrossRules} class, which processes
     * financial data for technical analysis. The {@code LowPriceIndicator} corresponds
     * to the low price values used to evaluate trading rules or strategies based on
     * price movements and trends.
     */
    protected LowPriceIndicator lowPrice;

    /**
     * Represents the close price indicator used in calculating and validating
     * trading rules within the context of EMA (Exponential Moving Average)
     * cross strategies.
     * <p>
     * This field provides access to the closing prices of a financial instrument
     * as an essential component for technical analysis and rule evaluation.
     * <p>
     * The closePrice indicator is typically utilized to perform operations such
     * as calculating EMA values, determining conditions for market entry and
     * exit, and facilitating the development of more complex trading rules.
     */
    protected ClosePriceIndicator closePrice;

    /**
     * Represents the volume indicator used for analyzing trading volumes
     * within the context of EmaCrossRules-based strategies.
     * The volume indicator provides insights into the quantity of
     * assets traded in a given time period, supporting rule-based
     * entry and exit decisions in the market.
     */
    protected VolumeIndicator volume;

    /**
     * Represents a 20-period Exponential Moving Average (EMA) indicator.
     * This indicator is commonly used for tracking short-term market trends
     * in financial time series data by applying a weighted perspective
     * to more recent values.
     */
    protected EMAIndicator ema20;

    /**
     * The EMAIndicator representing the 50-period Exponential Moving Average (EMA).
     * This indicator is used to track the average price of a security over a specified
     * period of time, giving more weight to recent prices. It is commonly used in
     * technical analysis to identify trends and potential reversal points.
     */
    protected EMAIndicator ema50;

    /**
     * Represents a 200-period Exponential Moving Average (EMA) indicator used to calculate
     * the smoothed average of the close prices over a defined period of 200 bars.
     * This indicator provides insights into long-term trends in a financial instrument's price data.
     * <p>
     * It is part of the trading strategy logic encapsulated within the EmaCrossRules class,
     * and is used alongside shorter-term EMAs or other technical indicators to analyze and generate
     * market entry or exit signals.
     */
    protected EMAIndicator ema200;

    /**
     * Represents the MACD (Moving Average Convergence Divergence) indicator
     * used to analyze price trends and momentum in the market.
     * This field specifically calculates the difference between a shorter-term
     * EMA (Exponential Moving Average) and a longer-term EMA to generate the MACD line.
     * It is commonly used in financial and trading applications to identify potential
     * trend reversals or confirm the strength of a trend.
     */
    protected MACDIndicator macdLine;

    /**
     * Represents the Relative Strength Index (RSI) indicator used for technical analysis.
     * This indicator measures the strength and speed of a market's price movement
     * to identify overbought or oversold conditions.
     */
    protected RSIIndicator rsi;

    /**
     * Represents the simple moving average (SMA) indicator for volume data
     * in a financial time series. This indicator tracks the average volume
     * over a specified number of periods and is used to analyze volume trends
     * as part of the trading strategy.
     */
    protected SMAIndicator avgVolume;

    // EMA 200 Distance Filter
    // |close - ema200| <= (ema200DistPerc / 100) * close
    // We can use a custom rule or transform indicators
    // Let's use a custom rule for simplicity in logic
    /**
     * Rule that evaluates whether the distance condition related to the
     * 200-period Exponential Moving Average (EMA) is met.
     * Typically used to determine if certain market conditions are favorable
     * for entry or exit based on the proximity of prices to the EMA200.
     */
    protected Rule ema200DistMet;

    /**
     * Represents a trading rule evaluating whether the trading volume
     * meets specified criteria in the context of EMA cross rules.
     * This rule can be used as a condition for triggering trading actions.
     */
    protected Rule volumeMet;

    /**
     * A rule that determines whether entry is allowed based on the date.
     * <p>
     * This rule can be configured to evaluate conditions related to time or date
     * for permitting entry actions, such as placing a trade or executing a specific strategy.
     * It is part of the set of rules in the EmaCrossRules class that influence strategy decisions.
     */
    protected Rule allowEntryDate;

    /**
     * Constructs the EmaCrossRules object, which defines entry and exit rules for trading based on EMA crossovers,
     * volume filters, distance from EMA200, and other technical indicators.
     *
     * @param configuration the configuration object containing parameters for EMA lengths, volume filters,
     *                      MACD parameters, and other rule settings
     * @param barSeries     the BarSeries object that holds the series of bars (candlesticks) used for calculating indicators
     *                      and applying trading rules
     */
    protected EmaCrossRules(EmaCrossConfiguration configuration, BarSeries barSeries) {

        this.configuration = configuration;
        this.barSeries = barSeries;

        this.openPrice = new OpenPriceIndicator(barSeries);
        this.highPrice = new HighPriceIndicator(barSeries);
        this.lowPrice = new LowPriceIndicator(barSeries);
        this.closePrice = new ClosePriceIndicator(barSeries);
        this.volume = new VolumeIndicator(barSeries);

        this.ema20 = new EMAIndicator(closePrice, configuration.getEma20Length());
        this.ema50 = new EMAIndicator(closePrice, configuration.getEma50Length());
        this.ema200 = new EMAIndicator(closePrice, configuration.getEma200Length());

        this.macdLine = new MACDIndicator(closePrice, configuration.getMacdFastLength(), configuration.getMacdSlowLength());

        this.rsi = new RSIIndicator(closePrice, 14);

        // 4. Volume Filter
        this.avgVolume = new SMAIndicator(volume, configuration.getVolAvgPeriod());

        // 5. EMA 200 Distance Filter
        // |close - ema200| <= (ema200DistPerc / 100) * close
        // We can use a custom rule or transform indicators
        // Let's use a custom rule for simplicity in logic
        this.ema200DistMet = configuration.isUseEma200DistanceFilter() ?
                (index, tradingRecord) -> {
                    Num close = closePrice.getValue(index);
                    Num ema = ema200.getValue(index);
                    Num dist = close.minus(ema).abs();
                    Num maxDist = close.multipliedBy(barSeries.numFactory().numOf(configuration.getEma200DistPerc())).dividedBy(barSeries.numFactory().numOf(100));
                    return dist.isLessThanOrEqual(maxDist);
                } :
                new BooleanRule(true);

        // Rules
        // volume > volMultiplier * avgVolume
        this.volumeMet = configuration.isUseVolumeFilter() ?
                (index, tradingRecord) -> {
                    Num vol = volume.getValue(index);
                    Num avg = avgVolume.getValue(index);
                    return vol.isGreaterThan(avg.multipliedBy(barSeries.numFactory().numOf(configuration.getVolMultiplier())));
                } :
                new BooleanRule(true);

        allowEntryDate = (index, tradingRecord) -> barSeries.getBar(index).getBeginTime().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(configuration.getEntryDate());
    }

    /**
     * Calculates the angle of the slope between the current and previous values
     * of the provided EMA (Exponential Moving Average) indicator.
     *
     * @param index the index of the current bar in the series
     * @param ema20 the EMAIndicator instance from which values are derived
     * @return the angle of the slope in degrees
     */
    protected static double calculateAngle(int index, EMAIndicator ema20) {
        Num current = ema20.getValue(index);
        Num previous = ema20.getValue(index - 1);
        double slope = current.minus(previous).dividedBy(previous).multipliedBy(ema20.getBarSeries().numFactory().numOf(100)).doubleValue();
        return Math.atan(slope) * 180.0 / Math.PI;
    }

    /**
     * Retrieves the exit rule for the trading strategy defined in the EmaCrossRules class.
     * This rule determines the conditions under which an existing trade position should be exited.
     *
     * @return the Rule object representing the exit conditions of the trading strategy
     */
    public abstract Rule getExitRule();

    /**
     * Retrieves the entry rule for the trading strategy defined in the EmaCrossRules class.
     * This rule determines the conditions under which a trade position should be opened.
     *
     * @return the Rule object representing the entry conditions of the trading strategy
     */
    public abstract Rule getEntryRule();
}

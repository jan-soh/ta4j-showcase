package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.Trade;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

/**
 * The EmaCrossShortRules class defines a set of rules for short trading strategies
 * based on exponential moving averages (EMA) and other technical indicators.
 * It extends the EmaCrossRules class and customizes its behavior to generate
 * short entry and exit signals using specific conditions.
 * <p>
 * This class incorporates the following components:
 * <p>
 * 1. **Entry Signal**:
 * - Determines when EMA 20 crosses below EMA 50 as an entry signal for short trades.
 * <p>
 * 2. **Filters**:
 * - EMA 200 Filter: Ensures the close price is below EMA 200 for short trade eligibility.
 * - RSI Filter: Utilizes the Relative Strength Index (RSI) value to validate conditions
 * for short trades based on a specified threshold.
 * - MACD Filter: Uses the MACD (Moving Average Convergence Divergence) indicator
 * to meet short trade criteria.
 * - EMA Slope Filter: Checks the slope of EMA 20 to ensure the trend meets the
 * configuration's slope threshold for short trades.
 * <p>
 * 3. **Exit Strategy**:
 * - Utilizes an EMA-based undercut threshold for closing short positions.
 * - Includes logic for fixed stop-loss based on the entry candle's properties.
 * <p>
 * The class operates with the following key methods:
 * <p>
 * - **getEntryRule**: Provides the compiled entry rule based on the EMA cross and
 * applied filters for short trades.
 * - **getExitRule**: Specifies the conditions under which a short position
 * should be exited.
 * <p>
 * The internal method `initRules` initializes all the rules and filters used
 * by the entry and exit conditions, allowing flexibility through a configurable setup.
 */
public class EmaCrossShortRules extends EmaCrossRules {

    // --- Entry Signal: EMA 20 crossing EMA 50 ---
    /**
     * Represents a trading rule that checks when the 20-period Exponential Moving Average (EMA)
     * crosses below the 50-period Exponential Moving Average (EMA). This rule is used as a condition
     * to identify potential short-selling opportunities in a financial market strategy.
     */
    private Rule ema20CrossBelow50;

    // --- Filters ---
    // 1. EMA 200 Filter
    /**
     * Represents the rule indicating whether the short condition
     * based on the 200-period Exponential Moving Average (EMA) is met.
     * This is primarily used in the context of defining entry conditions
     * for short trades within the EMA cross-over strategy.
     */
    private Rule ema200ShortMet;

    // 2. RSI Filter
    /**
     * Represents the Relative Strength Index (RSI) condition used as part of the short entry rules
     * in the EMA Cross Short Strategy.
     * <p>
     * This rule is designed to evaluate whether RSI-based conditions are met for triggering
     * a short entry signal. It is combined with other conditions within the strategy to determine
     * entry points into the market.
     * <p>
     * Part of the internal rule set for defining technical criteria in the short strategy logic.
     */
    private Rule rsiMet;

    // 3. MACD Filter
    /**
     * A rule indicating whether the MACD (Moving Average Convergence Divergence) condition
     * has been met for the specific trading strategy. This rule is used within the context
     * of short trading strategies to evaluate and validate entry or exit conditions
     * based on MACD indicators.
     */
    private Rule macdMet;

    // 6. EMA Slope Filter
    // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
    /**
     * Represents a rule that evaluates whether the exponential moving average (EMA) slope criteria
     * for initiating a long position has been met. This rule is part of the conditions required
     * for short-exit or strategy alignment in the EMA Cross Short trading strategy.
     */
    private Rule emaSlopeLongMet;

    // Exit strategy: EMA 20 with undercut
    // Long: Close < EMA 20 * (1 - d/100)
    // Short: Close > EMA 20 * (1 + d/100)
    /**
     * Represents the rule that determines the conditions under which a short position exit is triggered.
     * This rule is based on specific criteria defined in the context of the EMA cross strategy.
     * It is initialized as part of the rule setup process within the EmaCrossShortRules class.
     */
    private Rule shortExitMet;

    // --- Final Entry Conditions ---
    /**
     * Represents the main entry rule for the EMA crossover short trading strategy.
     * This rule combines various technical indicators and conditions to define
     * the criteria for entering a short position in the market.
     * <p>
     * The rule is initialized during the setup of the EmaCrossShortRules class
     * and is evaluated against the provided BarSeries data to determine if the
     * conditions for entering a short trade are met.
     */
    private Rule entryRuleShort;

    /**
     * Constructs an instance of EmaCrossShortRules with the specified configuration and bar series.
     * Initializes the rules for the EMA cross short strategy.
     *
     * @param configuration the EMA cross configuration containing the settings for the strategy
     * @param barSeries     the bar series representing the market data on which the strategy operates
     */
    public EmaCrossShortRules(EmaCrossConfiguration configuration, BarSeries barSeries) {
        super(configuration, barSeries);
        initRules();
    }


    /**
     * Retrieves the entry rule for the EMA Cross short strategy.
     *
     * @return the Rule object representing the conditions for entry in the short strategy
     */
    public Rule getEntryRule() {
        return entryRuleShort;
    }

    /**
     * Retrieves the exit rule for the EMA Cross short strategy.
     *
     * @return the Rule object representing the conditions for exit in the short strategy
     */
    public Rule getExitRule() {
        return shortExitMet;
    }

    /**
     * Initializes the rules for the EMA cross short strategy. Sets up both entry and exit
     * conditions based on various indicators and filters.
     * <p>
     * Entry conditions:
     * 1. A short entry is triggered when the EMA 20 crosses below EMA 50.
     * 2. Optional filters are applied based on the configuration:
     * - EMA 200 filter: Ensures the close price is below EMA 200 if enabled.
     * - RSI filter: Ensures RSI is below a configured threshold if enabled.
     * - MACD filter: Ensures MACDLine is below a configured threshold if enabled.
     * - EMA Slope filter: Ensures the slope of EMA 20 meets the configured threshold if enabled.
     * 3. The entry rule consolidates all these conditions and ensures all must be satisfied
     * to trigger an entry.
     * <p>
     * Exit conditions:
     * 1. Short positions are exited when either:
     * - The close price moves above the EMA 20 by a threshold percentage (EMA exit).
     * - The price exceeds a limit based on the entry candle's height and a configured
     * multiplier (fixed stop-loss condition).
     * <p>
     * Internal behavior:
     * - Dynamically computes EMA slopes and angles for checks.
     * - Handles position type checks to differentiate exit conditions between long and short positions.
     * - Ensures compatibility with configured filters for a customizable strategy.
     */
    private void initRules() {

        // --- Entry Signal: EMA 20 crossing EMA 50 ---
        this.ema20CrossBelow50 = new CrossedDownIndicatorRule(ema20, ema50);

        // --- Filters ---
        // 1. EMA 200 Filter
        this.ema200ShortMet = configuration.isUseEma200Filter() ? new UnderIndicatorRule(closePrice, ema200) : new BooleanRule(true);

        // 2. RSI Filter
        this.rsiMet = configuration.isUseRsiFilter() ? new UnderIndicatorRule(rsi, configuration.getRsiThreshold()) : new BooleanRule(true);

        // 3. MACD Filter
        this.macdMet = configuration.isUseMacdFilter() ? new UnderIndicatorRule(macdLine, configuration.getMacdThreshold()) : new BooleanRule(true);

        // 6. EMA Slope Filter
        // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
        this.emaSlopeLongMet = configuration.isUseEmaSlopeFilter() ?
                (index, tradingRecord) -> {
                    if (index < 1) return false;
                    double angle = 180 - calculateAngle(index, ema20);
                    return angle >= configuration.getEmaSlopeThreshold();
                } :
                new BooleanRule(true);

        this.entryRuleShort = (index, tradingRecord) -> {

            if (!ema200ShortMet.isSatisfied(index, tradingRecord)) {
                return false;
            }
            if (!rsiMet.isSatisfied(index, tradingRecord)) {
                return false;
            }
            if (!macdMet.isSatisfied(index, tradingRecord)) {
                return false;
            }
            if (!volumeMet.isSatisfied(index, tradingRecord)) {
                return false;
            }
            if (!ema200DistMet.isSatisfied(index, tradingRecord)) {
                return false;
            }
            if (!emaSlopeLongMet.isSatisfied(index, tradingRecord)) {
                return false;
            }
            if (!ema20CrossBelow50.isSatisfied(index, tradingRecord)) {
                return false;
            }
            if (!allowEntryDate.isSatisfied(index, tradingRecord)) {
                return false;
            }
            return true;
        };

        shortExitMet = (index, tradingRecord) -> {

            // never exit long positions with this rule
            boolean isLongPosition = Trade.TradeType.BUY == tradingRecord.getCurrentPosition().getStartingType();
            if (isLongPosition) {
                return false;
            }

            Num close = closePrice.getValue(index);
            Num ema = ema20.getValue(index);
            Num undercutFactor = barSeries.numFactory().numOf(1).plus(barSeries.numFactory().numOf(configuration.getTpUndercutPerc()).dividedBy(barSeries.numFactory().numOf(100)));
            boolean emaExit = close.isGreaterThan(ema.multipliedBy(undercutFactor));


            boolean pHeightExit = false;
            if (tradingRecord.getCurrentPosition().isOpened()) {
                int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
                if (index > entryIndex && entryIndex < barSeries.getBarCount()) {
                    // this is the fixed SL: close position, if the price drops below "position entry price" minus n times "the entry candle height".
                    Num oO = openPrice.getValue(entryIndex); // entry open price!
                    Num hO = highPrice.getValue(entryIndex); // entry high price!
                    Num lO = lowPrice.getValue(entryIndex); // entry low price!
                    Num lH = highPrice.getValue(index); // current high!
                    Num h = lO.minus(hO).abs();
                    Num mult = barSeries.numFactory().numOf(configuration.getSlMultiplier());
                    Num limit = oO.plus(h.multipliedBy(mult));
                    pHeightExit = lH.isGreaterThan(limit);
                }
            }

            return emaExit || pHeightExit;
        };
    }
}

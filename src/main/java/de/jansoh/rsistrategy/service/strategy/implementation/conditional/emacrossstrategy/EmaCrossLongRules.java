package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.Trade;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

/**
 * The EmaCrossLongRules class defines a set of trading rules
 * for identifying entry and exit signals in a long trading strategy
 * based on moving averages (EMA), RSI, MACD, and other technical indicators.
 * <p>
 * This class extends from the base class EmaCrossRules and focuses specifically
 * on the long side of the strategy. It implements the various entry conditions,
 * filters, and exit logic required for validating long trade setups.
 */
public class EmaCrossLongRules extends EmaCrossRules {

    // --- Entry Signal: EMA 20 crossing EMA 50 ---
    /**
     * Represents a trading rule that triggers when the 20-period Exponential Moving Average (EMA20)
     * crosses above the 50-period Exponential Moving Average (EMA50).
     * <p>
     * This rule is intended to be used as part of a larger strategy to identify potential upward
     * trends in a financial instrument. It serves as an entry condition for long positions
     * when the short-term moving average (EMA20) surpasses the longer-term moving average (EMA50),
     * indicating potential bullish momentum.
     */
    private Rule ema20CrossAbove50;

    // --- Filters ---
    // 1. EMA 200 Filter
    /**
     * Represents the rule that verifies if the conditions for entering a
     * long trade based on the EMA 200 indicator are met.
     * <p>
     * This rule is a critical component in the trading decision-making process
     * for identifying long entry points when the price behavior aligns with
     * the EMA 200 strategy defined in the configuration.
     * <p>
     * Used in conjunction with other rules within the {@code EmaCrossLongRules} class
     * to form a comprehensive long trade entry strategy.
     */
    private Rule ema200LongMet;

    // 2. RSI Filter
    /**
     * Represents the rule for checking if the RSI (Relative Strength Index) condition is met.
     * This rule is used as part of a broader set of conditions to determine signal entry or exit
     * in the context of an EMA (Exponential Moving Average) crossover strategy.
     * <p>
     * The rule evaluates the RSI indicator against predefined thresholds, ensuring that trades
     * are executed only when RSI-based conditions align with the strategy's requirements.
     * <p>
     * This field is initialized within the {@code initRules} method of the containing class.
     */
    private Rule rsiMet;

    // 3. MACD Filter
    /**
     * Represents the rule that evaluates the conditions based on the MACD (Moving Average Convergence Divergence) indicator
     * used in the long entry strategy within the EMA cross strategy.
     * <p>
     * The rule is designed to determine whether the MACD indicator meets the criteria specified in the configuration
     * for initiating a long trade. The MACD is typically used to assess momentum and trend direction.
     */
    private Rule macdMet;

    // 6. EMA Slope Filter
    // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
    /**
     * Represents a rule that determines whether the slope of a specified EMA (Exponential Moving Average)
     * satisfies conditions for a potential long entry signal.
     * <p>
     * The rule evaluates the slope of the EMA to ascertain upward momentum in the market.
     * It is typically used as part of a broader set of rules to validate trading signals
     * in strategies such as EMA crossovers.
     */
    private Rule emaSlopeLongMet;

    // Exit strategy: EMA 20 with undercut
    // Long: Close < EMA 20 * (1 - d/100)
    // Short: Close > EMA 20 * (1 + d/100)
    /**
     * Represents the rule that determines the conditions for exiting a long position
     * in the EMA cross strategy. This rule is evaluated as part of the overall strategy
     * logic to identify when a long position should be exited, based on specific market
     * indicators or conditions.
     */
    private Rule longExitMet;

    // --- Final Entry Conditions ---
    /**
     * Represents the entry rule for initiating long trades based on the EMA crossover strategy.
     * This rule is composed of multiple conditions that must be satisfied to allow trade entries.
     * The conditions typically involve technical indicators such as EMA, MACD, RSI,
     * volume filters, and other configurable parameters.
     * <p>
     * This rule is defined and initialized within the context of the EmaCrossLongRules class,
     * which extends EmaCrossRules. It leverages the strategy configuration and the bar series
     * to create a composite rule tailored for entering long positions.
     * <p>
     * The entry rule logic takes into account:
     * - Exponential Moving Averages (EMAs) of various lengths.
     * - Momentum indicators such as MACD and RSI.
     * - Volume-based filters.
     * - Configurable date restrictions.
     */
    private Rule entryRuleLong;

    /**
     * Constructor for the EmaCrossLongRules class, which initializes the rules for the EMA cross long strategy.
     *
     * @param configuration The configuration settings for the EMA cross strategy.
     * @param barSeries     The series of price bars used for rule evaluation.
     */
    public EmaCrossLongRules(EmaCrossConfiguration configuration, BarSeries barSeries) {
        super(configuration, barSeries);
        initRules();
    }

    /**
     * Retrieves the entry rule used for the EMA cross long strategy.
     *
     * @return the entry rule, which determines when an entry condition is met for a trade.
     */
    public Rule getEntryRule() {
        return entryRuleLong;
    }

    /**
     * Retrieves the exit rule used for the EMA cross long strategy.
     *
     * @return the exit rule, which determines when an exit condition is met for a trade.
     */
    public Rule getExitRule() {
        return longExitMet;
    }

    private void initRules() {

        // --- Entry Signal: EMA 20 crossing EMA 50 ---
        this.ema20CrossAbove50 = new CrossedUpIndicatorRule(ema20, ema50);

        // --- Filters ---
        // 1. EMA 200 Filter
        this.ema200LongMet = configuration.isUseEma200Filter() ? new OverIndicatorRule(closePrice, ema200) : new BooleanRule(true);

        // 2. RSI Filter
        this.rsiMet = configuration.isUseRsiFilter() ? new OverIndicatorRule(rsi, configuration.getRsiThreshold()) : new BooleanRule(true);

        // 3. MACD Filter
        this.macdMet = configuration.isUseMacdFilter() ? new OverIndicatorRule(macdLine, configuration.getMacdThreshold()) : new BooleanRule(true);

        // 6. EMA Slope Filter
        // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
        this.emaSlopeLongMet = configuration.isUseEmaSlopeFilter() ?
                (index, tradingRecord) -> {
                    if (index < 1) return false;
                    double angle = calculateAngle(index, ema20);
                    return angle >= configuration.getEmaSlopeThreshold();
                } :
                new BooleanRule(true);

        this.entryRuleLong = (index, tradingRecord) -> {

            if (!ema200LongMet.isSatisfied(index, tradingRecord)) {
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
            if (!ema20CrossAbove50.isSatisfied(index, tradingRecord)) {
                return false;
            }
            if (!allowEntryDate.isSatisfied(index, tradingRecord)) {
                return false;
            }
            return true;
        };

        longExitMet = (index, tradingRecord) -> {

            // never exit short positions with this rule
            boolean isShortPosition = Trade.TradeType.SELL == tradingRecord.getCurrentPosition().getStartingType();
            if (isShortPosition) {
                return false;
            }

            Num close = closePrice.getValue(index);
            Num ema = ema20.getValue(index);
            Num undercutFactor = barSeries.numFactory().numOf(1).minus(barSeries.numFactory().numOf(configuration.getTpUndercutPerc()).dividedBy(barSeries.numFactory().numOf(100)));
            boolean emaExit = close.isLessThan(ema.multipliedBy(undercutFactor));


            boolean pHeightExit = false;
            if (tradingRecord.getCurrentPosition().isOpened()) {
                int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
                if (index > entryIndex && entryIndex < barSeries.getBarCount()) {
                    // this is the fixed SL: close position, if the price drops below "position entry price" minus n times "the entry candle height".
                    Num oO = openPrice.getValue(entryIndex); // entry open price!
                    Num hO = highPrice.getValue(entryIndex); // entry high price!
                    Num lO = lowPrice.getValue(entryIndex); // entry low price!
                    Num lC = lowPrice.getValue(index); // current low!
                    Num h = lO.minus(hO).abs();
                    Num mult = barSeries.numFactory().numOf(configuration.getSlMultiplier());
                    Num limit = oO.minus(h.multipliedBy(mult));
                    pHeightExit = lC.isLessThan(limit);
                }
            }

            return emaExit || pHeightExit;
        };
    }
}

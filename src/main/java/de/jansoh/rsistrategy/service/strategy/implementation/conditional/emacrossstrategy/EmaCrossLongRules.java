package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.Trade;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;

import java.time.LocalDate;

public class EmaCrossLongRules extends EmaCrossRules {

    // --- Entry Signal: EMA 20 crossing EMA 50 ---
    private Rule ema20CrossAbove50;
    private Rule ema20CrossBelow50;

    // --- Filters ---
    // 1. EMA 200 Filter
    private Rule ema200LongMet;
    private Rule ema200ShortMet;

    // 2. RSI Filter
    private RSIIndicator rsi;
    private Rule rsiMet;
    private Rule rsiShortMet;

    // 3. MACD Filter
    private MACDIndicator macdLine;
    private Rule macdMet;
    private Rule macdShortMet;

    // 4. Volume Filter
    private SMAIndicator avgVolume;

    // Rules
    // volume > volMultiplier * avgVolume
    private Rule volumeMet;
    // 5. EMA 200 Distance Filter
    // |close - ema200| <= (ema200DistPerc / 100) * close
    // We can use a custom rule or transform indicators
    // Let's use a custom rule for simplicity in logic
    private Rule ema200DistMet;


    // 6. EMA Slope Filter
    // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
    private Rule emaSlopeLongMet;
    private Rule emaSlopeShortMet;

    // Exit strategy: EMA 20 with undercut
    // Long: Close < EMA 20 * (1 - d/100)
    // Short: Close > EMA 20 * (1 + d/100)
    private Rule longExitMet;
    private Rule shortExitMet;

    // --- Final Entry Conditions ---
    private Rule entryRuleLong;

    private Rule entryRuleShort;

    private LocalDate entryDate = LocalDate.of(2025, 9, 4);
    private Rule allowEntryDate;

    public EmaCrossLongRules(EmaCrossConfiguration configuration, BarSeries barSeries) {
        super(configuration, barSeries);
    }


    public Rule getEntryRule() {
        return entryRuleLong;
    }

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


            // Candle after entry condition
            boolean pHeightExit = false;
            if (tradingRecord.getCurrentPosition().isOpened()) {
                int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
                int candleAfterEntryIndex = entryIndex;
                if (index > candleAfterEntryIndex && candleAfterEntryIndex < barSeries.getBarCount()) {
                    Num p = closePrice.getValue(candleAfterEntryIndex);
                    Num h = highPrice.getValue(candleAfterEntryIndex).minus(lowPrice.getValue(candleAfterEntryIndex));
                    Num limit = p.minus(h.multipliedBy(barSeries.numFactory().numOf(configuration.getSlMultiplier())));
                    pHeightExit = close.isLessThan(limit);
                }
            }

            return emaExit || pHeightExit;
        };
    }
}

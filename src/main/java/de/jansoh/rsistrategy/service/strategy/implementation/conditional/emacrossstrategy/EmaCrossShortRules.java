package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.Trade;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

public class EmaCrossShortRules extends EmaCrossRules {

    // --- Entry Signal: EMA 20 crossing EMA 50 ---
    private Rule ema20CrossBelow50;

    // --- Filters ---
    // 1. EMA 200 Filter
    private Rule ema200ShortMet;

    // 2. RSI Filter
    private Rule rsiMet;

    // 3. MACD Filter
    private Rule macdMet;

    // 6. EMA Slope Filter
    // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
    private Rule emaSlopeLongMet;

    // Exit strategy: EMA 20 with undercut
    // Long: Close < EMA 20 * (1 - d/100)
    // Short: Close > EMA 20 * (1 + d/100)
    private Rule shortExitMet;

    // --- Final Entry Conditions ---
    private Rule entryRuleShort;

    public EmaCrossShortRules(EmaCrossConfiguration configuration, BarSeries barSeries) {
        super(configuration, barSeries);
        initRules();
    }


    public Rule getEntryRule() {
        return entryRuleShort;
    }

    public Rule getExitRule() {
        return shortExitMet;
    }

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

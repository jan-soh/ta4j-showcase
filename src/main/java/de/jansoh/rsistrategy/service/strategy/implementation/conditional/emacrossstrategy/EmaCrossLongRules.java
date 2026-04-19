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

public class EmaCrossLongRules extends EmaCrossRules {

    // --- Entry Signal: EMA 20 crossing EMA 50 ---
    private Rule ema20CrossAbove50;

    // --- Filters ---
    // 1. EMA 200 Filter
    private Rule ema200LongMet;

    // 2. RSI Filter
    private RSIIndicator rsi;
    private Rule rsiMet;

    // 3. MACD Filter
    private MACDIndicator macdLine;
    private Rule macdMet;

    // 4. Volume Filter
    private SMAIndicator avgVolume;

    // 5. EMA 200 Distance Filter
    // |close - ema200| <= (ema200DistPerc / 100) * close
    // We can use a custom rule or transform indicators
    // Let's use a custom rule for simplicity in logic
    private Rule ema200DistMet;


    // 6. EMA Slope Filter
    // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
    private Rule emaSlopeLongMet;

    // Exit strategy: EMA 20 with undercut
    // Long: Close < EMA 20 * (1 - d/100)
    // Short: Close > EMA 20 * (1 + d/100)
    private Rule longExitMet;

    // --- Final Entry Conditions ---
    private Rule entryRuleLong;

    public EmaCrossLongRules(EmaCrossConfiguration configuration, BarSeries barSeries) {
        super(configuration, barSeries);
        initRules();
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

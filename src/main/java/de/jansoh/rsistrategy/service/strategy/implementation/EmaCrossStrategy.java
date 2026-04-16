package de.jansoh.rsistrategy.service.strategy.implementation;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

public class EmaCrossStrategy {

    /*
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
     */
    public static Strategy buildStrategy(BarSeries series,
                                         int emaTriggerLength,
                                         int emaFilterLength,
                                         boolean useEmaFilterLong,
                                         boolean useEmaFilterShort,
                                         boolean allowLong,
                                         boolean allowShort) {

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        OpenPriceIndicator openPrice = new OpenPriceIndicator(series);

        EMAIndicator ema50 = new EMAIndicator(closePrice, emaTriggerLength);
        EMAIndicator emaFilter = new EMAIndicator(closePrice, emaFilterLength);

        // Long entry: Price opens below EMA 50 and closes above
        Rule emaCrossAbove = new UnderIndicatorRule(openPrice, ema50)
                .and(new OverIndicatorRule(closePrice, ema50));

        Rule emaFilterLongMet = useEmaFilterLong ? new OverIndicatorRule(closePrice, emaFilter) : new BooleanRule(true);
        Rule entryRuleLong = allowLong ? emaCrossAbove.and(emaFilterLongMet) : new BooleanRule(false);

        // Short entry: Price opens above EMA 50 and closes below
        Rule emaCrossBelow = new OverIndicatorRule(openPrice, ema50)
                .and(new UnderIndicatorRule(closePrice, ema50));

        Rule emaFilterShortMet = useEmaFilterShort ? new UnderIndicatorRule(closePrice, emaFilter) : new BooleanRule(true);
        Rule entryRuleShort = allowShort ? emaCrossBelow.and(emaFilterShortMet) : new BooleanRule(false);

        // Use a dummy exit rule for now, as we'll handle TP/SL manually to match ATR-based logic exactly
        return new BaseStrategy("EMA Cross Strategy",
                entryRuleLong.or(entryRuleShort),
                new BooleanRule(false));
    }
}

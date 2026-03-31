package de.jansoh.rsistrategy.strategy;

import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.rules.*;
import org.ta4j.core.num.Num;

public class EmaCrossStrategy {

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

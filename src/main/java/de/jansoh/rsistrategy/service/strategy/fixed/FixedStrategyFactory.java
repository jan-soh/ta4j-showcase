package de.jansoh.rsistrategy.service.strategy.fixed;

import de.jansoh.rsistrategy.service.strategy.implementation.fixed.emacrossstrategy.EmaCrossStrategy;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

public class FixedStrategyFactory {
    public Strategy createDefault(BarSeries barSeries) {
        return EmaCrossStrategy.buildStrategy(barSeries, 50, 200, true, true, true, true);
    }
}

package de.jansoh.rsistrategy.service.strategy;

import de.jansoh.rsistrategy.service.strategy.implementations.EmaCrossStrategy;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

@Service
public class StrategyFactory {

    public Strategy create(BarSeries barSeries) {
        return EmaCrossStrategy.buildStrategy(barSeries, 45, 192, true, true, true, true);
    }

    public Strategy createDefault(BarSeries barSeries) {
        return EmaCrossStrategy.buildStrategy(barSeries, 50, 200, true, true, true, true);
    }
}

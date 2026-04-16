package de.jansoh.rsistrategy.service.strategy;

import de.jansoh.rsistrategy.service.strategy.implementation.AdvancedStrategy;
import de.jansoh.rsistrategy.service.strategy.implementation.EmaCrossAdvancedStrategy;
import de.jansoh.rsistrategy.service.strategy.implementation.EmaCrossStrategy;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

@Service
public class AdvancedStrategyFactory {

    public AdvancedStrategy create(BarSeries barSeries) {
        return new EmaCrossAdvancedStrategy(barSeries,
                20, 50, 200,
                true,
                false, 50,
                false, 0.0, 12, 26,
                true, 2, 8,
                true, 1.5,
                false, 0.01,
                0.25,
                2.0,
                true, true);
    }

    public Strategy createDefault(BarSeries barSeries) {
        return EmaCrossStrategy.buildStrategy(barSeries, 50, 200, true, true, true, true);
    }
}

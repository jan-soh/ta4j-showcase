package de.jansoh.rsistrategy.service.strategy.conditional;

import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.EmaCrossConfiguration;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.FastEmaCrossingSlowEmaStrategy;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;

@Service
public class ConditionalStrategyFactory {

    public ConditionalStrategy create(BarSeries barSeries) {
        return new FastEmaCrossingSlowEmaStrategy(barSeries);
    }

    public ConditionalStrategy createTest(BarSeries barSeries) {
        EmaCrossConfiguration config = new EmaCrossConfiguration();
        config.setDefaults();
        return new FastEmaCrossingSlowEmaStrategy(barSeries, config);
    }
}

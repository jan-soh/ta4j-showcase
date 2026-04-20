package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;

@Component
public class FastEmaCrossingSlowEmaStrategyFactory {

    public ConditionalStrategy create(EmaCrossConfiguration configuration, BarSeries barSeries) {

        return new FastEmaCrossingSlowEmaStrategy(barSeries, configuration);
    }
}

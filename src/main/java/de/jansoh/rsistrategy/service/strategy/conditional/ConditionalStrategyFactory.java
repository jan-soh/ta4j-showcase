package de.jansoh.rsistrategy.service.strategy.conditional;

import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.FastEmaCrossingSlowEmaStrategy;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;

@Service
public class ConditionalStrategyFactory {

    public ConditionalStrategy create(BarSeries barSeries) {
        return new FastEmaCrossingSlowEmaStrategy(barSeries);
    }
}

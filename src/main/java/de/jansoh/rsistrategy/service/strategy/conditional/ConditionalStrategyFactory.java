package de.jansoh.rsistrategy.service.strategy.conditional;

import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.FastEmaCrossingSlowEmaStrategy;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;

/**
 * Factory class for creating instances of {@code ConditionalStrategy}.
 * This class provides methods to create different configurations of
 * the {@code FastEmaCrossingSlowEmaStrategy}.
 */
@Service
public class ConditionalStrategyFactory {

    /**
     * Creates an instance of {@code ConditionalStrategy} using the provided {@code BarSeries}.
     *
     * @param barSeries the time series data used for initializing the strategy.
     *                  Contains price bars which serve as input for strategy rules and calculations.
     * @return an instance of {@code ConditionalStrategy}, specifically a {@code FastEmaCrossingSlowEmaStrategy}.
     */
    public ConditionalStrategy create(BarSeries barSeries) {
        return new FastEmaCrossingSlowEmaStrategy(barSeries);
    }
}

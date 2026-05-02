package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;

/**
 * Factory class for creating instances of the FastEmaCrossingSlowEmaStrategy.
 * This strategy is designed to evaluate trading opportunities by determining
 * when a fast Exponential Moving Average (EMA) crosses a slow EMA within a
 * given bar series based on the provided configuration.
 * <p>
 * This class abstracts the creation of strategy objects to ensure consistency
 * and encapsulation of initialization logic.
 */
@Component
public class FastEmaCrossingSlowEmaStrategyFactory {

    /**
     * Creates an instance of the FastEmaCrossingSlowEmaStrategy using the provided configuration
     * and bar series. The strategy determines trade opportunities by identifying
     * when a fast Exponential Moving Average (EMA) crosses a slow EMA.
     *
     * @param configuration the configuration settings for the EMA crossover strategy
     * @param barSeries     the bar series containing the historical price data to evaluate
     * @return a ConditionalStrategy instance configured with the given parameters
     */
    public ConditionalStrategy create(EmaCrossConfiguration configuration, BarSeries barSeries) {

        return new FastEmaCrossingSlowEmaStrategy(barSeries, configuration);
    }
}

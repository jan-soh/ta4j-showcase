package de.jansoh.rsistrategy.service.strategy.conditional;

import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.service.strategy.StrategyConfiguration;
import de.jansoh.rsistrategy.service.strategy.tpsl.SlStrategy;
import de.jansoh.rsistrategy.service.strategy.tpsl.TpStrategy;

/**
 * Represents a conditional trading strategy that defines rules and configurations
 * for long and short positions. This interface extends the {@code TpStrategy}
 * and {@code SlStrategy} interfaces to support take-profit (TP) and stop-loss (SL)
 * calculations.
 * <p>
 * A concrete implementation of this interface is expected to determine entry
 * and exit conditions for both long and short trades based on specific rules
 * and configurations.
 */
public interface ConditionalStrategy extends TpStrategy, SlStrategy {

    /**
     * Evaluates whether the entry conditions for a long position are satisfied
     * at the specified index within a trading strategy.
     *
     * @param index the index in the time series data to evaluate the long entry condition.
     *              Typically represents a point in a BarSeries or trading data set.
     * @return {@code true} if the long entry conditions are met at the specified index,
     * {@code false} otherwise.
     */
    boolean isLongEntrySatisfied(int index);

    /**
     * Evaluates whether the exit conditions for a long position are satisfied
     * at the specified index within a trading strategy.
     *
     * @param index    the index in the time series data to evaluate the long exit condition.
     *                 This typically represents a point in a BarSeries or trading data set.
     * @param position the current position for which the long exit condition is being evaluated.
     * @return {@code true} if the long exit conditions are met at the specified index,
     * {@code false} otherwise.
     */
    boolean isLongExitSatisfied(int index, Position position);

    /**
     * Evaluates whether the entry conditions for a short position are satisfied
     * at the specified index within a trading strategy.
     *
     * @param index the index in the time series data to evaluate the short entry condition.
     *              Typically represents a point in a BarSeries or trading data set.
     * @return {@code true} if the short entry conditions are met at the specified index,
     * {@code false} otherwise.
     */
    boolean isShortEntrySatisfied(int index);

    /**
     * Evaluates whether the exit conditions for a short position are satisfied
     * at the specified index within a trading strategy.
     *
     * @param index    the index in the time series data to evaluate the short exit condition.
     *                 Typically represents a point in a BarSeries or trading data set.
     * @param position the current position for which the short exit condition is being evaluated.
     * @return {@code true} if the short exit conditions are met at the specified index,
     * {@code false} otherwise.
     */
    boolean isShortExitSatisfied(int index, Position position);

    /**
     * Retrieves the configuration associated with the conditional trading strategy.
     *
     * @return an instance of {@code StrategyConfiguration} that encapsulates
     * the configuration details of the strategy, such as asset trade windows
     * and other implementation-specific settings.
     */
    StrategyConfiguration getConfiguration();
}

package de.jansoh.rsistrategy.service.strategy.tpsl;

import de.jansoh.rsistrategy.model.Position;
import org.ta4j.core.Bar;

import java.math.BigDecimal;

/**
 * Represents a strategy for calculating the stop-loss (SL) price for a given position.
 * <p>
 * Implementations of this interface define the logic for determining the stop-loss level
 * for a specific trading position based on the provided entry bar and position details.
 */
public interface SlStrategy {

    /**
     * Calculates the stop-loss (SL) price for a given trading position.
     *
     * @param positionEntry the bar representing the entry point of the position
     * @param position      the trading position for which the stop-loss is being calculated
     * @return the calculated stop-loss price as a BigDecimal
     */
    BigDecimal getSl(Bar positionEntry, Position position);
}

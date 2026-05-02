package de.jansoh.rsistrategy.service.strategy.tpsl;

import de.jansoh.rsistrategy.model.Position;
import org.ta4j.core.Bar;

import java.math.BigDecimal;

/**
 * The {@code TpStrategy} interface defines the contract for implementing
 * a Take Profit (TP) strategy in a trading system. A TP strategy is used
 * to determine the optimal take profit price for a given position based
 * on the entry data and other factors of a trading strategy.
 * <p>
 * Implementations of {@code TpStrategy} provide the mechanism to calculate
 * the target price at which a trade position would be closed to secure profit.
 * The calculation is typically based on the initial position entry and
 * the state of the trading position.
 */
public interface TpStrategy {

    /**
     * Calculates the target price (Take Profit) for a given trading position based
     * on the position entry and the current state of the position.
     * <p>
     *
     * @param positionEntry the market data (bar) representing the entry point of the position
     * @param position      the trading position for which the take profit is being calculated
     * @return the calculated take profit price as a BigDecimal
     */
    BigDecimal getTp(Bar positionEntry, Position position);
}

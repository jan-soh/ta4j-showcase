package de.jansoh.rsistrategy.model;

/**
 * Represents the type of an order in a trading system.
 * <p>
 * MARKET - An order to buy or sell at the current market price.
 * LIMIT - An order to buy or sell at a specified price or better.
 */
public enum OrderType {
    MARKET,
    LIMIT,
    TAKE_PROFIT_MARKET,
    STOP_MARKET;
}

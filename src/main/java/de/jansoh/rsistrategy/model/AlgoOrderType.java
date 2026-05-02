package de.jansoh.rsistrategy.model;

/**
 * Represents the type of an algorithmic order within a trading system.
 * This enum is used to specify whether the order is a Take Profit (TP)
 * or Stop Loss (SL) type.
 *
 * <ul>
 * <li>TP - Indicates a Take Profit order type.</li>
 * <li>SL - Indicates a Stop Loss order type.</li>
 * </ul>
 * <p>
 * The AlgoOrderType is often associated with other trading system components,
 * such as orders and positions, to help manage risk and realize profits.
 */
public enum AlgoOrderType {
    TP,
    SL
}

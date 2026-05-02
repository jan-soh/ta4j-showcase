package de.jansoh.rsistrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * The AlgoOrder class represents an algorithmic order used in trading strategies.
 * It contains details about the order, such as its unique identifiers, symbol,
 * quantity, timeframe, algorithm type, and trigger price.
 * <p>
 * This class is typically used to define take-profit (TP) or stop-loss (SL) algorithmic orders
 * associated with trading positions, allowing for automated execution of strategies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoOrder {

    /**
     * Represents a unique identifier for an algorithmic order.
     * This identifier is used to track and differentiate individual orders
     * within trading strategies or financial systems.
     */
    private String orderId;

    /**
     * Represents a unique identifier for a client-side order.
     * This identifier is typically assigned by the client system
     * to distinguish and track individual orders separate from other
     * system-generated identifiers.
     */
    private String clientOrderId;

    /**
     * Represents the trading symbol associated with the algorithmic order.
     * The symbol is used to identify the trading instrument, such as a stock,
     * currency pair, or other financial asset. It typically follows market
     * conventions, for example, "BTCUSD" for the Bitcoin to US Dollar trading pair.
     */
    private String symbol;

    /**
     * Represents the quantity associated with the algorithmic order.
     * This value defines the amount of the trading asset involved in the order.
     * Typically expressed in terms of the smallest tradable unit of the asset,
     * it must conform to the precision and constraints of the trading system.
     */
    private BigDecimal quantity;

    /**
     * Represents the timeframe associated with the algorithmic trading order.
     * The timeframe defines the duration or interval for analysis,
     * such as 1 minute, 1 hour, 1 day, etc.
     * <p>
     * This variable uses the {@link Timeframe} enum, which encapsulates
     * predefined options for time intervals commonly used in trading strategies.
     * Each value in the enum is associated with a unique shortcut (e.g., "1m", "1h")
     * and its corresponding duration in minutes.
     * <p>
     * The timeframe is critical in determining the granularity of the data
     * and the execution strategy of the algorithmic order.
     */
    private Timeframe timeframe;

    /**
     * Represents a unique identifier for the specific algorithm associated with the order.
     * This identifier is used to track and manage the algorithmic logic or strategy being
     * applied to the order, distinguishing it from other algorithms in the system.
     */
    private String algoId;

    /**
     * Specifies the type of algorithmic order associated with the trading position.
     * This variable is of type {@link AlgoOrderType}, which defines the valid types
     * of algorithmic orders that can be used in trading strategies.
     * <p>
     * The possible values for this variable are:
     * - {@code TP}: Take-Profit orders, which are designed to close a position
     * automatically when the price reaches a specified profit target.
     * - {@code SL}: Stop-Loss orders, which are designed to close a position
     * automatically to limit losses when the price moves against the trade.
     * <p>
     * This variable is critical for distinguishing between the different goals
     * of algorithmic orders in automated trading systems.
     */
    private AlgoOrderType type;

    /**
     * Represents the price at which a specific condition is triggered
     * for a trading algorithm. This value is typically used in algorithms
     * to activate certain trade actions, such as placing orders or
     * closing positions, once the specified price is reached.
     */
    private BigDecimal triggerPrice;

}

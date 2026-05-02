package de.jansoh.rsistrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a request to perform an algorithmic order on Binance.
 * This class encapsulates the properties required to create and send an
 * algorithmic order, such as stop loss or take profit, to the Binance API.
 * It serves as a data model for configuring and sending a variety of trading
 * algorithm operations.
 * <p>
 * The class supports the use of different order types, handling parameters
 * such as trigger price, stop price, and working type that are specific
 * to algorithmic trading operations. It also includes fields for uniquely
 * identifying client orders and setting timestamps for request validity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinanceAlgoOrderRequest {

    /**
     * Specifies the type of algorithmic order to be executed.
     * <p>
     * This field indicates the particular algorithm used for a trading operation,
     * such as stop-loss, take-profit, or other predefined order strategies.
     * It provides flexibility for configuring and identifying the trading logic
     * applied to specific orders within the Binance API.
     */
    private String algoType;

    /**
     * Represents the trading symbol associated with the algorithmic order request.
     * <p>
     * The symbol is used to specify the market pair (e.g., "BTCUSDT") on which the
     * order will be executed. It serves as a key identifier for the trading asset
     * combination, enabling operations specific to the chosen market.
     */
    private String symbol;

    /**
     * Specifies the order side (e.g., "BUY" or "SELL") for the algorithmic order request.
     * <p>
     * The value indicates the direction of the trade operation and determines whether
     * the order is intended to purchase or sell the specified trading asset in the market.
     * It plays a critical role in defining the trading action associated with the
     * Binance algorithmic order.
     */
    private String side;

    /**
     * Specifies the position side for the algorithmic order request.
     * <p>
     * This field is used to indicate the position direction of the order,
     * typically in the context of futures trading. Common values include "LONG" and "SHORT,"
     * representing the two possible directional stances for a specific position.
     * <p>
     * "LONG" indicates a buying or upward positioning expectation,
     * while "SHORT" refers to a selling or downward positioning expectation.
     * <p>
     * The value plays a critical role in determining the intent and behavior
     * of a trading operation and is often used in conjunction with
     * the order type and side to define the overall strategy.
     */
    private String positionSide;

    /**
     * Represents the type of order for an algorithmic trading request.
     * This field is used to define the specific type of order being executed,
     * such as market order, limit order, or other types as supported by the system.
     * It is critical for determining the behavior and execution details of the order.
     */
    private String type;

    /**
     * Represents the price at which the order will be triggered.
     * This value is typically used for conditional or algorithmic trading orders
     * where specific thresholds need to be met before execution.
     */
    private String triggerPrice;

    /**
     * Represents the quantity of the asset to be traded in the algorithmic order request.
     * This field specifies the amount to be bought or sold as part of the order.
     * The value is typically provided as a string to accommodate precision requirements.
     */
    private String quantity;

    /**
     * Represents the stop price for an order, typically used as a threshold price
     * to trigger certain order types, such as stop-loss or stop-limit orders.
     * This value determines when the associated action, such as placing or
     * modifying an order, is initiated based on market conditions.
     */
    private String stopPrice;

    /**
     * Specifies the method used to calculate the working price for an order
     * in the trading system. Typically used to define whether the working price
     * is determined by the "MARK_PRICE" (market price) or the "CONTRACT" (specific contract price).
     * This variable plays a crucial role in defining price-related
     * behaviors for algorithmic trading orders.
     */
    private String workingType;

    /**
     * Represents a flag to indicate whether price protection is enabled for the algorithmic order request.
     * Price protection is a mechanism to prevent execution when the market price deviates significantly.
     */
    private String priceProtect;

    /**
     * Represents the close position status as a string. This variable is used
     * to indicate whether a position will be closed as part of the algorithmic
     * order request.
     * <p>
     * Possible values and their interpretation depend on the specific implementation
     * within the BinanceAlgoOrderRequest context.
     */
    private String closePosition;

    /**
     * Specifies the maximum time window in milliseconds during which a request is considered valid.
     * Used to manage the validity duration of a request for synchronization with the server.
     */
    private Long recvWindow;

    /**
     * A unique identifier provided by the client to distinguish
     * and track a specific algorithmic order. This identifier is
     * typically used to correlate client-side request data with
     * server-side responses and ensure idempotency for order requests.
     * <p>
     * It is required to be unique for each order and is crucial
     * for identifying and managing algorithmic orders in trading
     * systems.
     */
    private String clientAlgoId;

    /**
     * The timestamp when the request is created or issued.
     * It is typically used to synchronize requests with the server to avoid
     * issues caused by time discrepancies between client and server.
     */
    private Long timestamp;
}

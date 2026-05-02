package de.jansoh.rsistrategy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;


/**
 * Represents a trading order with associated metadata and state.
 * <p>
 * This entity is mapped to the "trade_order" table in the database and contains details about
 * a specific trade order, including order identifiers, trade specifications, quantities, prices,
 * commissions, and order status.
 * <p>
 * Fields Overview:
 * - id: The unique identifier of the order.
 * - clientOrderId: The client-generated order identifier.
 * - symbol: The trading symbol associated with the order.
 * - timeframe: The timeframe associated with the order, e.g., 1m, 5m, etc.
 * - side: Specifies whether the order is a buy or sell operation.
 * - positionSide: Specifies the position side, e.g., LONG, SHORT, or BOTH.
 * - type: The type of order, such as MARKET or LIMIT.
 * - originalQuantity: The total quantity specified for the order.
 * - lastFilledQuantity: The quantity filled in the most recent execution of this order.
 * - filledAccumulatedQuantity: The cumulative quantity filled for the order.
 * - originalPrice: The initial price specified for the order.
 * - averagePrice: The average price of all executed trades for the order.
 * - lastFilledPrice: The price of the most recent execution of this order.
 * - realizedProfit: The profit realized from this order's execution.
 * - commissionAsset: The asset in which the commission is charged.
 * - commission: The commission charged for the order.
 * - orderStatus: The current status of the order, e.g., NEW, FILLED, PARTIALLY_FILLED.
 * - orderId: The unique identifier assigned by the server for this order.
 * - orderTradeTime: The timestamp associated with the order's trade.
 */
@Entity
@Table(name = "trade_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * The unique identifier for the order entity.
     * <p>
     * This field is automatically generated using the IDENTITY strategy, ensuring
     * a unique value is assigned to each order upon persistence. It serves as the
     * primary key within the "trade_order" table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The client-generated identifier for the order.
     * <p>
     * This field uniquely identifies an order from the client's perspective, allowing
     * external systems or clients to track and manage the order independently of its
     * server-generated identifier. It is particularly useful for reconciling orders
     * between client and server or for implementing custom business logic that relies
     * on client-driven identifiers.
     */
    private String clientOrderId;

    /**
     * Represents the trading symbol associated with the order.
     * <p>
     * This field specifies the financial instrument or market being traded, such as
     * a stock ticker, currency pair, or other tradeable entities. It is used for
     * identifying and processing the order within the relevant market or exchange.
     */
    private String symbol;

    /**
     * Specifies the timeframe associated with the order.
     * <p>
     * This field represents the temporal duration of a trading operation and is linked
     * to predefined intervals such as 1m (One Minute), 5m (Five Minutes), 1h (One Hour), etc.
     * Timeframes define the granularity at which trading data is aggregated or analyzed.
     * <p>
     * The value of this field is an enum constant of {@link Timeframe} and is persisted
     * as a string in the database using the {@code EnumType.STRING} mapping.
     */
    @Enumerated(EnumType.STRING)
    private Timeframe timeframe;

    /**
     * Specifies whether the trading order is a buy or sell operation.
     * <p>
     * This field captures the direction of the trade. It can either be:
     * - BUY: Indicates a purchase of the specified asset.
     * - SELL: Indicates a sale of the specified asset.
     * <p>
     * The value is represented as an {@link OrderSide} enum and is persisted
     * in the database as a string using the {@code EnumType.STRING} mapping.
     */
    @Enumerated(EnumType.STRING)
    private OrderSide side;

    /**
     * Specifies the position side of the trading order.
     * <p>
     * This field determines the directional intent of the position associated
     * with the trade. It is primarily used in futures or margin trading contexts,
     * where positions can have different sides. The possible values are:
     * - LONG: Indicates a long position, i.e., buying with an expectation that the asset's price will increase.
     * - SHORT: Indicates a short position, i.e., selling with an expectation that the asset's price will decrease.
     * - BOTH: Indicates that the position can have both long and short sides, typically in hedging scenarios.
     * <p>
     * The value is represented as a {@link PositionSide} enum and is persisted
     * as a string in the database using the {@code EnumType.STRING} mapping.
     */
    @Enumerated(EnumType.STRING)
    private PositionSide positionSide;

    /**
     * Specifies the type of order, such as MARKET or LIMIT.
     * <p>
     * This field determines the pricing strategy or behavior of the order:
     * - MARKET: Represents an order to be executed immediately at the current market price.
     * - LIMIT: Represents an order to be executed at a specified price or better.
     * <p>
     * The value is represented as an {@link OrderType} enum and is persisted as a string
     * in the database using the {@code EnumType.STRING} mapping. This allows for readable
     * storage of the order type in the database.
     */
    @Enumerated(EnumType.STRING)
    private OrderType type;

    /**
     * Represents the total quantity of the trade order as initially specified.
     * <p>
     * This field defines the original quantity of the asset to be traded when
     * the order is created. It serves as a reference for the trade's intended size
     * and does not account for any partial executions or modifications. The value
     * is stored using high precision to accommodate financial calculations.
     * <p>
     * Constraints:
     * - Precision: Up to 20 digits.
     * - Scale: Up to 10 digits after the decimal point.
     * <p>
     * Mapped to a database column with precision and scale constraints to ensure
     * accurate storage of financial quantities.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal originalQuantity;

    /**
     * Represents the quantity filled in the most recent execution of the order.
     * <p>
     * This field tracks the amount of the order that was fulfilled in the last execution
     * of the trade. It provides granular information about the progress of the order's
     * fulfillment and is updated with every new trade execution associated with the order.
     * <p>
     * Constraints:
     * - Precision: Up to 20 digits.
     * - Scale: Up to 10 digits after the decimal point.
     * <p>
     * Mapped to a database column with precision and scale constraints to ensure accurate
     * storage and representation of financial quantities.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal lastFilledQuantity;

    /**
     * Represents the total accumulated quantity that has been filled for a specific transaction
     * or order. This value is stored as a BigDecimal to support high precision and scaling,
     * ensuring accuracy in calculations involving large or fractional quantities.
     * <p>
     * The precision is set to 20 digits, and the scale is set to 10 digits, meaning the value can
     * have up to 20 digits in total, with 10 of those digits allocated for the fractional part.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal filledAccumulatedQuantity;

    /**
     * Represents the original price of an item or product.
     * This value is stored with high precision and scale to accommodate
     * large and fractional monetary values.
     * <p>
     * Precision: Specifies the total number of significant digits (20).
     * Scale: Specifies the number of digits to the right of the decimal point (10).
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal originalPrice;

    /**
     * Represents the average price value with a high level of precision and scale.
     * <p>
     * The precision is set to 20, indicating the maximum number of total digits
     * allowed, while the scale is set to 10, specifying the number of digits
     * allowed after the decimal point.
     * <p>
     * This variable is typically used to store the computed average value of prices
     * for financial or business calculations requiring exact representations.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal averagePrice;

    /**
     * Represents the price at which the last trade or transaction was filled.
     * This value is stored with a precision of 20 digits and a scale of 10
     * digits for maintaining high accuracy in financial calculations.
     * <p>
     * It is typically used in trading or financial systems to track the
     * most recent executed price for an asset or a security.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal lastFilledPrice;

    /**
     * Represents the actual profit that has been earned and realized, typically
     * calculated after the completion of a financial transaction or operation.
     * This variable is stored with high precision to accommodate detailed
     * financial calculations.
     * <p>
     * annotated with {@code @Column(precision = 20, scale = 10)} to specify
     * database column precision and scale, where:
     * - Precision (20) defines the maximum number of digits that can be stored.
     * - Scale (10) defines the number of digits stored to the right of the decimal point.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal realizedProfit;

    /**
     * Represents the asset in which the commission is calculated or paid.
     * This variable holds the identifier or symbol of the asset used for
     * processing commissions in transactions or operations.
     */
    private String commissionAsset;

    /**
     * Represents a commission value with high precision and scale.
     * <p>
     * The commission is stored as a BigDecimal with a precision of 20 and a scale of 10,
     * allowing for accurate representation of decimal values suitable for financial
     * or mathematical calculations.
     * <p>
     * Constraints:
     * - Precision: The total number of digits that the value can have, including both integer and fractional parts.
     * - Scale: The number of digits allowed after the decimal point.
     * <p>
     * This variable is mapped to a database column with the specified precision and scale using
     * the @Column annotation.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal commission;

    /**
     * Represents the current status of an order.
     * The status is stored as a string enumeration value.
     * This variable is typically used to track and manage the state of an order
     * throughout its lifecycle.
     */
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    /**
     * Represents the unique identifier for an order.
     * This field is used to track and differentiate individual orders in the system.
     */
    private String orderId;

    /**
     * Represents the date and time when the order was traded.
     * This variable stores the timestamp in a time-zone-aware
     * format using ZonedDateTime, which ensures accurate handling
     * of different time zones.
     */
    private ZonedDateTime orderTradeTime;
}

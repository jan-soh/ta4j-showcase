package de.jansoh.rsistrategy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Represents a trade position in a trading system.
 * This entity maps to the "trade_position" table in the database.
 * It includes details about the order, position side, quantity,
 * take profit and stop loss algorithms, and other relevant attributes.
 * <p>
 * The class provides methods for checking the existence of take profit
 * or stop loss algorithm orders, updating realized profit, and
 * determining the position side (long or short).
 * <p>
 * It uses Lombok annotations for boilerplate code generation, such as
 * getters, setters, builders, and constructors.
 */
@Entity
@Table(name = "trade_position")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    /**
     * The unique identifier for the trade position entity.
     * This field is marked as the primary key of the "trade_position" database table.
     * <p>
     * The value is automatically generated using the identity strategy, where the
     * underlying database generates a unique value for each record insertion.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Represents the unique identifier for the associated trading order.
     * This field stores the order ID as a string, which can be used to
     * reference or correlate the position with a specific order in the
     * trading system or external trading platforms (such as brokers or APIs).
     */
    private String orderId;

    /**
     * Represents the symbol associated with the trading position.
     * This field typically holds the unique identifier or ticker symbol
     * for the financial instrument being traded (e.g., "AAPL" for Apple Inc.,
     * "BTCUSDT" for Bitcoin/US Dollar pair).
     * <p>
     * It is a key attribute used to identify the specific instrument
     * for which the position was opened, and it is often correlated
     * with trading orders and market data.
     */
    private String symbol;

    /**
     * Represents the timeframe associated with a trading position.
     * This field defines the granularity or interval of time used for
     * analyzing or executing trades associated with the position.
     *
     * <ul>
     *   <li>The timeframe is represented as an enumerated type, providing
     *       predefined intervals such as minutes, hours, days, or weeks.</li>
     *   <li>Examples of timeframes include "1m" (one minute), "1h" (one hour),
     *       and "1d" (one day).</li>
     *   <li>Each value in the enum corresponds to a shortcut string
     *       and a duration in minutes.</li>
     * </ul>
     * <p>
     * The value of this field is persisted as a string in the database
     * due to the use of {@code EnumType.STRING}.
     */
    @Enumerated(EnumType.STRING)
    private Timeframe timeframe;

    /**
     * Represents the side of the trading position, indicating its orientation in the market.
     * The possible values are:
     * <ul>
     *   <li>{@code LONG}: Indicates that the position is a long position, where the trader
     *       anticipates the price of the asset to increase.</li>
     *   <li>{@code SHORT}: Indicates that the position is a short position, where the trader
     *       anticipates the price of the asset to decrease.</li>
     *   <li>{@code BOTH}: Indicates that the position involves both long and short sides,
     *       typically in complex trading strategies or hedging scenarios.</li>
     * </ul>
     * This field is stored as a string in the database due to the use of {@code EnumType.STRING}.
     * It is a key attribute for determining the market direction and behavior of the position.
     */
    @Enumerated(EnumType.STRING)
    private PositionSide side;

    /**
     * Represents the quantity of the trade position.
     * <ul>
     *   <li>This field defines the amount of the financial instrument held in the position.</li>
     *   <li>Stored as a {@code BigDecimal} to ensure precision when dealing with large numbers or fractions.</li>
     *   <li>The database column is defined with a precision of 20 and a scale of 10, allowing up to 20 digits with 10 digits after the decimal point.</li>
     *   <li>Initialized to {@code BigDecimal.ZERO} by default, indicating no quantity.</li>
     * </ul>
     */
    @Builder.Default
    @Column(precision = 20, scale = 10)
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * Represents the unique identifier for the take-profit algorithm associated with the trading position.
     * This field stores an optional string value which serves as a reference or key
     * for the algorithm used to determine the take-profit logic.
     * <p>
     * Key characteristics:
     * <ul>
     *   <li>If not {@code null}, it indicates that a take-profit algorithm exists for this position.</li>
     *   <li>It is used to identify and correlate the position with a specific take-profit strategy or rule set
     *       that is implemented in the trading system.</li>
     * </ul>
     * The presence of a value in this field can be verified using the {@code hasTpAlgoOrder()} method.
     */
    private String tpAlgoId;

    /**
     * Represents the unique identifier for the take-profit client order associated with the trading position.
     * This field contains an optional string value that serves as a reference or key
     * for the client order linked to the take-profit logic for the position.
     * <p>
     * Key characteristics:
     * - If not {@code null}, it indicates that a specific client order exists for the take-profit mechanism of this position.
     * - It is used to identify and correlate the position with a particular take-profit client order
     * in the trading system or an external exchange.
     * - Acts as a unique identifier for tracking and managing the take-profit order’s lifecycle.
     */
    private String tpClientOrderId;

    /**
     * Indicates whether the take-profit order associated with the trading position
     * has been filled.
     * <p>
     * Key characteristics:
     * - A value of {@code true} signifies that the take-profit order was successfully executed.
     * - A value of {@code false} indicates that the take-profit order has not yet been filled
     * or executed.
     * - This field is typically used to track the status of the take-profit mechanism for the
     * trading position.
     */
    private boolean tpOrderFilled;

    /**
     * Represents the algorithmically calculated price associated with a specific trading or financial
     * operation. The value is stored as a {@code BigDecimal} to ensure precision, especially for
     * financial calculations requiring accuracy with significant decimal places.
     * <p>
     * The field is annotated with {@code @Column} to specify database column properties:
     * - {@code precision = 20}: Maximum number of digits the number can have, including integer and fractional parts.
     * - {@code scale = 10}: Number of digits reserved for the fractional part.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal tpAlgoPrice;

    /**
     * Represents the identifier for a specific algorithm in the system.
     * This ID is used to uniquely distinguish between different algorithms.
     * It is expected to be a non-null and non-empty string.
     */
    private String slAlgoId;

    /**
     * Represents the unique identifier for a client order in the system.
     * This variable is used to track and differentiate orders placed by clients.
     */
    private String slClientOrderId;

    /**
     * Indicates whether the stop-loss order has been filled.
     * This variable is set to {@code true} when the stop-loss order is successfully executed,
     * and {@code false} otherwise.
     */
    private boolean slOrderFilled;

    /**
     * Represents the stop-loss algorithm price used in trading systems.
     * The value is stored with a precision of up to 20 digits and a scale of 10 decimal places.
     * This field is typically used to define a price level at which a stop-loss action is triggered.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal slAlgoPrice;

    /**
     * Represents the unique identifier for a client's order.
     * This ID is typically assigned by the client to track and manage their orders.
     */
    private String clientOrderId;

    /**
     * Represents the average open price of a financial instrument, calculated
     * as a BigDecimal to maintain high precision and scale for accurate
     * representation of decimal values. The column is configured to have a
     * precision of 20 and a scale of 10, allowing up to 20 digits in total,
     * with 10 digits after the decimal point.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal averageOpenPrice;

    /**
     * Represents the average price at which trades were closed.
     * The value is stored with a precision of 20 digits and a scale of 10,
     * ensuring high accuracy for financial calculations.
     */
    @Column(precision = 20, scale = 10)
    private BigDecimal averageClosedPrice;

    /**
     * Represents the realized profit amount in a financial calculation or transaction.
     * The value is stored with high precision to accommodate financial data,
     * with a maximum of 20 digits, including up to 10 decimal places.
     * By default, the value is initialized to zero.
     */
    @Builder.Default
    @Column(precision = 20, scale = 10)
    private BigDecimal realizedProfit = BigDecimal.ZERO;

    /**
     * Represents the specific date and time when a particular event, activity, or process begins.
     * The time is stored with time-zone information to account for differences across various regions.
     */
    private ZonedDateTime openTime;

    /**
     * Represents the date and time when a specific operation or process was closed.
     * The timestamp is stored with time zone information to ensure precise representation
     * across different regions.
     */
    private ZonedDateTime closedTime;

    /**
     * Represents the index or position of an entry within a collection or data structure.
     * This variable is marked as transient, meaning it will not be serialized
     * as part of the object's persistent state.
     */
    @Transient
    private int entryIndex;

    /**
     * A transient flag indicating whether a particular entity or process
     * is marked as closed. This variable is not persisted in the database
     * and is primarily used for in-memory logic or temporary state management.
     */
    @Transient
    private boolean markClosed;

    /**
     * Indicates whether the resource, connection, or stream is currently closed.
     * This variable is set to true when the resource has been intentionally closed
     * and is no longer available for operations. Otherwise, it is false.
     */
    private boolean closed;

    /**
     * Checks if a Take-Profit algorithm order is associated.
     *
     * @return true if the Take-Profit algorithm identifier is not null, false otherwise.
     */
    public boolean hasTpAlgoOrder() {
        return tpAlgoId != null;
    }

    /**
     * Determines whether there is a stop-loss algorithm order based on the presence of an SL algorithm ID.
     *
     * @return true if the stop-loss algorithm ID (slAlgoId) is not null, false otherwise.
     */
    public boolean hasSlAlgoOrder() {
        return slAlgoId != null;
    }

    /**
     * Adds the specified realized profit to the current realized profit value.
     * If the input is null, the method returns without performing any operation.
     * If the current realized profit value is null, it initializes it to zero before adding.
     *
     * @param realizedProfit the realized profit to be added; must be a non-null BigDecimal value
     */
    public void addRealizedProfit(BigDecimal realizedProfit) {
        if (null == realizedProfit) {
            return;
        }
        if (null == this.realizedProfit) {
            this.realizedProfit = BigDecimal.ZERO;
        }

        this.realizedProfit = this.realizedProfit.add(realizedProfit);
    }

    /**
     * Checks if the current position side is set to LONG.
     *
     * @return true if the position side is LONG, otherwise false
     */
    public boolean isLong() {
        return PositionSide.LONG == side;
    }

    /**
     * Checks if the position side is SHORT.
     *
     * @return true if the position side is SHORT, false otherwise
     */
    public boolean isShort() {
        return PositionSide.SHORT == side;
    }
}

package de.jansoh.rsistrategy.ta4jbridge.model;

import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Position;
import org.ta4j.core.Trade;

/**
 * Represents a specialized trading record that extends the functionality of a
 * BaseTradingRecord. A trading record typically tracks all trades or positions
 * during the execution of a trading strategy, maintaining a history of such
 * operations.
 * <p>
 * TaTradingRecord provides additional constructors that allow for initialization
 * based on a predefined Position or custom trading configurations.
 * <p>
 * Constructor Details:
 * - The first constructor allows initialization of a trading record with a specific
 * Position instance. This can be used to track trading activity starting from an
 * existing position.
 * - The second constructor allows initialization using a name and a default trade
 * type (e.g., BUY or SELL). This is useful for categorizing or naming trading
 * records while specifying a default trading bias.
 */
public class TaTradingRecord extends BaseTradingRecord {

    /**
     * Constructs a new instance of TaTradingRecord with a pre-defined position.
     * This constructor allows tracking of an existing position within the
     * trading record, leveraging the functionality provided by the superclass.
     * <p>
     * A trading record is typically used to maintain a history of trades and
     * positions during the execution of a trading strategy. By initializing the
     * record with an existing position, users can incorporate previously established
     * trading activity into their analysis.
     *
     * @param position the position to initialize the trading record with; represents
     *                 an already defined entry and potentially exit trade for
     *                 tracking within the record
     */
    public TaTradingRecord(Position position) {
        super(position);
    }

    /**
     * Constructs a new instance of TaTradingRecord with a specified name
     * and default trade type. This constructor is intended for creating a
     * trading record categorized by a custom name, while specifying a default
     * trade type (e.g., BUY or SELL) that will apply to trades in the record.
     * <p>
     * A trading record is typically used for tracking trading activity, including
     * executed trades and open/closed positions, during the execution of a
     * trading strategy.
     *
     * @param name      the name to assign to this trading record; can be used
     *                  for categorization or identification of the record
     * @param tradeType the default trade type (e.g., BUY or SELL) that will
     *                  be associated with new trades created in this record
     */
    public TaTradingRecord(String name, Trade.TradeType tradeType) {
        super(name, tradeType);
    }
}

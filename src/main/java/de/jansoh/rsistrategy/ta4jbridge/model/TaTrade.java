package de.jansoh.rsistrategy.ta4jbridge.model;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTrade;

/**
 * Represents a trade in a financial trading strategy. This class extends
 * the BaseTrade class from the ta4j library, adding more specialized
 * behaviors or properties for usage in trading applications.
 * <p>
 * A trade is typically used to encapsulate the details of a single buy or sell
 * action within a trading strategy. Each trade contains information like the
 * index of the trade in the series, the associated bar series, and the trade type
 * (e.g., BUY or SELL).
 * <p>
 * Constructor Details:
 * - Initializes a trade with an index representing the position of the trade in
 * the bar series, the bar series itself for reference, and the trade type.
 */
public class TaTrade extends BaseTrade {

    /**
     * Constructs a new instance of TaTrade, representing a single trade within
     * a bar series. A trade encapsulates the index of the trade, the associated
     * bar series, and the type of trade (e.g., BUY or SELL).
     * <p>
     * This constructor delegates initialization to the superclass, ensuring all
     * required parameters are properly set. Trades are typically used in trading
     * strategy evaluations to record and analyze buy or sell actions.
     *
     * @param index  the position of the trade in the bar series, typically
     *               indicating the bar index where the trade occurs
     * @param series the bar series associated with this trade, representing
     *               the data structure containing time series of bars
     * @param type   the type of the trade, either BUY or SELL, indicating
     *               the trade action
     */
    public TaTrade(int index, BarSeries series, TradeType type) {
        super(index, series, type);
    }
}

package de.jansoh.rsistrategy.service.kline;

import de.jansoh.rsistrategy.model.Timeframe;
import org.ta4j.core.BarSeries;

/**
 * Represents an update event for Klines (candlesticks) containing the symbol, timeframe,
 * and updated bar series data. This interface can be implemented to handle updates
 * relevant to financial data, such as market charts or OHLC (Open, High, Low, Close) data.
 */
public interface KlinesUpdateEvent {

    /**
     * Retrieves the trading pair or financial instrument symbol associated with this update event.
     *
     * @return the symbol as a string representing the trading pair or financial instrument.
     */
    String getSymbol();

    /**
     * Retrieves the timeframe associated with this update event. The timeframe represents
     * a standardized time interval, such as one minute, one hour, or one day, and is
     * used to identify the granularity of the data in the event.
     *
     * @return the {@code Timeframe} object representing the predefined time interval.
     */
    Timeframe getTimeframe();

    /**
     * Retrieves the updated bar series data associated with this Klines update event.
     * The bar series represents a sequence of financial data points, typically used
     * for market analysis or chart representation with open, high, low, and close values.
     *
     * @return the {@code BarSeries} object containing the updated sequence of financial data.
     */
    BarSeries getBarSeries();
}

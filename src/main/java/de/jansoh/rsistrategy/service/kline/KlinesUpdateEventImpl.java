package de.jansoh.rsistrategy.service.kline;

import de.jansoh.rsistrategy.model.Timeframe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.ta4j.core.BarSeries;

/**
 * Implementation of the {@code KlinesUpdateEvent} interface, representing an update event for Klines
 * (candlesticks) that contains the trading symbol, timeframe, and updated bar series data.
 * This implementation can be used to handle real-time updates relevant to financial data,
 * such as market charts or OHLC (Open, High, Low, Close) data.
 * <p>
 * This class is designed to provide the functionality defined by the {@code KlinesUpdateEvent} interface,
 * including access to the trading symbol, the standardized timeframe, and the bar series data.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlinesUpdateEventImpl implements KlinesUpdateEvent {

    /**
     * Represents the trading symbol or financial instrument associated with this
     * Klines update event. The symbol is typically used to identify the specific
     * trading pair or market, such as "BTC/USD" or "AAPL".
     */
    private String symbol;

    /**
     * Represents the timeframe associated with the Klines (candlestick) update event.
     * The timeframe defines a standardized time interval, such as one minute, one hour,
     * or one day, used to determine the granularity of the financial data in the event.
     * <p>
     * The value is represented by the {@link Timeframe} enumeration, which provides
     * predefined time intervals and their respective durations.
     */
    private Timeframe timeframe;

    /**
     * Represents the updated bar series data in a Klines (candlestick) update event.
     * The {@code BarSeries} object contains a sequence of financial data points, typically
     * used for market analysis and charting. Each data point in the series includes information
     * such as open, high, low, and close prices, along with volume and time information.
     * <p>
     * This data is primarily used to represent dynamic market changes over a predefined timeframe,
     * facilitating analysis of trends, patterns, and trading opportunities.
     */
    private BarSeries barSeries;

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public Timeframe getTimeframe() {
        return timeframe;
    }

    @Override
    public BarSeries getBarSeries() {
        return barSeries;
    }
}

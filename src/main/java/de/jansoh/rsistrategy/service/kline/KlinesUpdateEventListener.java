package de.jansoh.rsistrategy.service.kline;

/**
 * A listener interface for receiving and handling updates to Klines (candlestick) data.
 * Implementations of this interface are used to process {@link KlinesUpdateEvent} instances
 * that contain updates for specific trading pairs, timeframes, and bar series data.
 */
public interface KlinesUpdateEventListener {

    /**
     * Handles updates for Klines (candlestick) data. This method is called when a
     * {@link KlinesUpdateEvent} is received, allowing implementations to process the
     * updated data for a specified trading pair, timeframe, and bar series.
     *
     * @param event the {@link KlinesUpdateEvent} containing the symbol, timeframe,
     *              and updated bar series data for the Klines update.
     */
    void onKlinesUpdate(KlinesUpdateEvent event);
}

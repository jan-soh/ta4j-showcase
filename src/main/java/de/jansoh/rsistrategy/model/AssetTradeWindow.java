package de.jansoh.rsistrategy.model;

import lombok.*;

/**
 * Represents a trading window for a specific asset with configurable parameters.
 * The AssetTradeWindow class contains information about a trading symbol,
 * the associated timeframe, and the leverage used for trades.
 * <p>
 * This class is designed to be used in trading applications where
 * users can configure asset-specific trading windows.
 * The {@link Timeframe} enumeration provides predefined intervals
 * for time-based trading analyses.
 * <p>
 * Features:
 * - Tracks the trading symbol for a particular asset.
 * - Supports multiple timeframes for trading intervals.
 * - Configurable leverage for trade computations or predictions.
 * <p>
 * An object of this class is typically used for defining trade
 * configurations before executing trading strategies or storing
 * trade window preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetTradeWindow {

    /**
     * The trading symbol representing a specific asset in the market.
     * This value is used to identify and associate trades, positions,
     * and configurations with the corresponding asset.
     * <p>
     * For example, in cryptocurrency trading, this could represent symbols
     * such as "BTCUSDT" or "ETHUSD", while in stock markets, it could
     * represent symbols like "AAPL" or "TSLA".
     * <p>
     * The `symbol` field is commonly used across various components, such as:
     * - Defining trade orders.
     * - Tracking active positions.
     * - Associating analysis or strategy data with the corresponding asset.
     * <p>
     * Note: The value of this field should conform to the format defined by
     * the specific market or trading platform being used.
     */
    private String symbol;

    /**
     * Defines the timeframe for trade operations within a specific trading window.
     * <p>
     * The `timeframe` field specifies the pre-defined interval for analyzing and
     * executing trading strategies. It is based on the {@link Timeframe} enumeration,
     * which provides a set of commonly used time intervals, such as 1 minute,
     * 5 minutes, 1 hour, up to 1 week.
     * <p>
     * This field is critical for determining the granularity of trade decisions,
     * allowing traders to align their strategies with specific market conditions
     * and analysis windows. Changes to this field directly impact trade
     * computations and strategy evaluations.
     */
    private Timeframe timeframe;

    /**
     * The leverage used for trades within this trading window.
     * <p>
     * This field represents the multiplier applied to the capital being traded.
     * It allows traders to control a larger position size with a smaller amount
     * of actual capital, which can amplify both potential profits and potential losses.
     * <p>
     * Key characteristics:
     * - A positive integer value that defines the degree of leverage.
     * - Excluded from {@code equals()} and {@code hashCode()} computations,
     * as indicated by the {@link EqualsAndHashCode.Exclude} annotation.
     * <p>
     * Note: Careful consideration should be taken when configuring leverage,
     * as it significantly affects the risk exposure in trading activities.
     */
    @EqualsAndHashCode.Exclude
    private int leverage;
}

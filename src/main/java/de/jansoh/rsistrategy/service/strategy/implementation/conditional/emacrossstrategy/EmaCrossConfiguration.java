package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.service.strategy.StrategyConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Configuration settings for an EMA (Exponential Moving Average) Cross strategy.
 * This class is used to define the parameters required for executing and managing
 * trading strategies based on EMA crossovers, as well as additional technical
 * indicators and filters.
 * The configuration provides flexible control over the behavior of the strategy.
 * <p>
 * Features include:
 * - EMA lengths for short, medium, and long-period calculations.
 * - Optional technical filters, including RSI, MACD, and volume-based conditions.
 * - Stop-loss and take-profit configurations.
 * - Support for long and short trades with corresponding risk management settings.
 * - Time-based entry settings for backtesting purposes.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmaCrossConfiguration implements StrategyConfiguration {

    /**
     * The length of the short-term Exponential Moving Average (EMA) used in the trading strategy.
     * This value determines the number of price bars considered when calculating the EMA,
     * typically used to identify short-term price trends.
     *
     * <ul>
     * Key characteristics include:
     * - A smaller value reacts more quickly to recent price changes.
     * - A larger value provides a smoother average, reducing sensitivity to short-term fluctuations.
     * </ul>
     * <p>
     * This parameter is commonly used in conjunction with longer-term EMAs (e.g., ema50Length, ema200Length)
     * to generate trading signals based on EMA crossovers or relationships.
     */
    protected int ema20Length;

    /**
     * Represents the length of the EMA (Exponential Moving Average) period used
     * for the calculation of the 50-period EMA in the EMA cross strategy configuration.
     * This variable is part of the configuration for determining the behavior
     * of the EMA cross strategy, particularly for identifying trends and
     * crossover points.
     * <p>
     * The value of this variable dictates how many recent data points are
     * considered when calculating the 50-period EMA, which is instrumental
     * in defining medium-term technical trends in the market.
     */
    protected int ema50Length;

    /**
     * Specifies the length of the EMA (Exponential Moving Average) with a period of 200.
     * This parameter determines the number of bars over which the EMA is calculated
     * and is commonly used in trading strategies to identify long-term trends.
     */
    protected int ema200Length;

    /**
     * Flag indicating whether the EMA 200 filter is enabled.
     * When enabled, trading rules consider the relative position or behavior of the
     * price in relation to the 200-period Exponential Moving Average (EMA).
     * This filter is commonly used to assess market trends or bias before
     * allowing entry or exit decisions in a trading strategy.
     */
    protected boolean useEma200Filter;

    /**
     * Indicates whether the RSI filter is enabled in the trading strategy configuration.
     * When enabled, the RSI filter applies a condition based on the Relative Strength Index (RSI)
     * to determine the validity of trade signals.
     */
    protected boolean useRsiFilter;

    /**
     * The RSI threshold value used as a key parameter to determine
     * conditions related to the use of an RSI-based trading filter
     * in the strategy configuration. If the RSI filter is enabled
     * in the configuration, this value represents the threshold that
     * the Relative Strength Index (RSI) must exceed or stay below
     * to make trading decisions.
     */
    protected int rsiThreshold;

    /**
     * Indicates whether the Moving Average Convergence Divergence (MACD) filter
     * should be applied in the strategy configuration.
     * <p>
     * The MACD filter is used to evaluate market trends based on the difference
     * between fast and slow moving averages and may help identify potential
     * trade setups if enabled.
     */
    protected boolean useMacdFilter;

    /**
     * Represents the threshold value used for evaluating the Moving Average Convergence Divergence (MACD) indicator
     * in trading strategy configurations. This variable is used to filter potential trade signals based on the
     * magnitude of the MACD value, enabling finer control over entry and exit rules. A larger threshold would
     * require stronger MACD signals for consideration, whereas a smaller threshold would include weaker signals.
     */
    protected double macdThreshold;

    /**
     * The length of the fast moving average used in the MACD calculation.
     * Determines the sensitivity of the MACD line by specifying the period
     * for calculating the exponential moving average (EMA) for the fast-moving component.
     * A smaller value increases the sensitivity, capturing quicker price movements.
     */
    protected int macdFastLength;

    /**
     * Defines the period length for calculating the slow moving average component
     * of the MACD (Moving Average Convergence Divergence) indicator used in the
     * EMA Cross strategy configuration. A larger value increases the smoothing by
     * considering more data points in the calculation.
     */
    protected int macdSlowLength;

    /**
     * Determines whether the volume filter is applied as part of the EMA cross
     * strategy configuration. When set to {@code true}, the strategy will only
     * consider trading opportunities where the bar's volume exceeds a specified
     * threshold defined in relation to the average volume.
     * <p>
     * This filter is used to ensure that trades are executed in periods of
     * significant market participation, as measured by volume.
     */
    protected boolean useVolumeFilter;

    /**
     * The volume multiplier used as part of a volume-based filtering rule
     * in the EMA cross trading strategy. This value is used to determine
     * whether the current volume exceeds a specified multiple of the average
     * volume over a configurable period.
     * <p>
     * The filtering rule typically checks if the current volume is greater
     * than the product of this multiplier and the average volume to confirm
     * sufficient market activity.
     */
    protected double volMultiplier;

    /**
     * Represents the period used for calculating the average volume in the strategy configuration.
     * This value is utilized in the volume filter condition, where the average volume over this
     * period is compared against the current volume to determine if a trade condition is met.
     */
    protected int volAvgPeriod;

    /**
     * Indicates whether the EMA 200 Distance Filter is enabled in the EMA Cross
     * strategy configuration. The EMA 200 Distance Filter imposes a constraint
     * based on the proximity of the current closing price to the EMA 200 value.
     * <p>
     * When enabled, the closing price must be within a specified percentage
     * distance (defined by the {@code ema200DistPerc} field) of the EMA 200
     * value for trade rules to be considered valid. This filter is designed
     * to ensure that trades only occur when price conditions closely
     * align with the specified trend indicator.
     */
    protected boolean useEma200DistanceFilter;

    /**
     * The percentage threshold used to evaluate the distance between the closing price
     * and the 200-period EMA (Exponential Moving Average).
     * <p>
     * This variable represents the allowable deviation (in percentage) between the
     * closing price of a financial instrument and its 200-period EMA for filtering
     * trading signals. The filter is active only when the `useEma200DistanceFilter`
     * configuration is enabled.
     * <p>
     * It is primarily utilized in conjunction with a rule that validates whether
     * the absolute difference between the closing price and the 200-period EMA
     * is within the defined percentage threshold:
     * <p>
     * |close - ema200| <= (ema200DistPerc / 100) * close
     */
    protected double ema200DistPerc;

    /**
     * Indicates whether the filter based on the slope of the EMA (Exponential Moving Average) is enabled.
     * This filter is designed to evaluate the rate of change or angle of the specified EMA to incorporate
     * slope-based conditions into trading strategies. When enabled, trading decisions may leverage
     * the calculated EMA slope to determine trends or momentum dynamics.
     */
    protected boolean useEmaSlopeFilter;

    /**
     * Represents the threshold value used to determine the slope of an Exponential Moving Average (EMA).
     * This variable is used as part of the EMA slope filter, which assesses the angle of the EMA movement.
     * <p>
     * A positive slope indicates upward momentum, while a negative slope indicates downward momentum.
     * The threshold helps to filter out entries or exits based on the steepness of the EMA slope.
     * <p>
     * It is primarily utilized in strategies where trend angle is a decisive factor
     * for making trading decisions.
     */
    protected double emaSlopeThreshold;

    /**
     * Specifies the percentage threshold for undercutting the take profit level.
     * This variable is used to fine-tune the take profit conditions of the strategy,
     * allowing for flexibility in exit rules. A value greater than 0 denotes the
     * percentage by which the specified take profit level can be adjusted downward
     * to potentially allow for more conservative profit taking.
     */
    protected double tpUndercutPerc;

    /**
     * Represents the multiplier used for stop-loss calculation in the EMA Cross
     * trading strategy. This value is applied to determine the distance at which
     * a stop-loss order will be placed based on specific trade criteria.
     * <p>
     * The stop-loss value is critical for managing risk during trading and helps
     * ensure that potential losses are minimized if the trade moves in an
     * unfavorable direction.
     * <p>
     * Adjusting this multiplier allows fine-tuning of the stop-loss distance
     * relative to market conditions and strategy configurations.
     */
    protected double slMultiplier;

    /**
     * A configuration flag that determines whether long trades
     * (buy positions) are allowed in the EMA Cross Strategy.
     * <p>
     * When set to {@code true}, the strategy is permitted to open long positions
     * based on the defined entry and exit rules. If set to {@code false}, the
     * strategy will avoid initiating any long trades regardless of other
     * conditions being met.
     */
    protected boolean allowLong;

    /**
     * Indicates whether the short trading strategy is enabled.
     * When set to {@code true}, the system allows the execution of short trades
     * based on the specified configuration and rules.
     * Typically used in conjunction with indicators and entry/exit rules to
     * determine the viability of short trading opportunities.
     */
    protected boolean allowShort;

    /**
     * Defines the trading window configured for a specific asset within the strategy.
     * <p>
     * This field is used to encapsulate the trade-specific configuration such as
     * symbol, timeframe, and leverage for the corresponding asset. The {@code AssetTradeWindow}
     * provides flexibility to execute trading strategies tailored to each asset's market conditions.
     * <p>
     * Use cases:
     * - Enables per-asset customization of trading parameters.
     * - Supports storing and applying strategies for different timeframes.
     * - Allows defining leverage per asset for managing trading risks.
     * <p>
     * This property plays a critical role in the configuration of trading strategies.
     * Proper setup ensures the trading strategy operates effectively for the targeted asset.
     */
    @JsonProperty("assetTradeWindow")
    private AssetTradeWindow assetTradeWindow;

    /**
     * Only used for testing purposes. Use this to allow trades only after a specific date.
     */
    protected LocalDate entryDate;

    /**
     * Resets the configuration fields of the EmaCrossConfiguration object to their default values.
     * <p>
     * The method initializes a collection of key configuration parameters often used
     * in trading strategies and sets them to predefined default states. These default
     * settings provide a baseline for the EMA cross strategy, ensuring that the object
     * is in a consistent state before use.
     * <p>
     * Default configurations include:
     * - Exponential Moving Average (EMA) lengths (e.g., 20, 50, 200 periods).
     * - Filters for EMA, RSI, MACD, volume, EMA slope, and EMA 200 distance.
     * - Thresholds, multipliers, parameters, and Boolean switches for each filter.
     * - Trade settings like Take Profit (TP) undercut percentage, Stop Loss (SL) multiplier,
     * allowable trade direction (long, short), and trade entry date.
     * - Asset-specific configuration encapsulated in an {@code AssetTradeWindow} object.
     * <p>
     * Note:
     * This method is intended to standardize and simplify initialization prior to
     * performing EMA-based trading strategy execution.
     */
    public void setDefaults() {
        this.ema20Length = 20;
        this.ema50Length = 50;
        this.ema200Length = 200;
        this.useEma200Filter = true;
        this.useRsiFilter = false;
        this.rsiThreshold = 50;
        this.useMacdFilter = false;
        this.macdThreshold = 0.0;
        this.macdFastLength = 12;
        this.macdSlowLength = 26;
        this.useVolumeFilter = true;
        this.volMultiplier = 2.0;
        this.volAvgPeriod = 8;
        this.useEma200DistanceFilter = true;
        this.ema200DistPerc = 1.5;
        this.useEmaSlopeFilter = false;
        this.emaSlopeThreshold = 0.01;
        this.tpUndercutPerc = 0.25;
        this.slMultiplier = 2.0;
        this.allowLong = true;
        this.allowShort = true;
        this.entryDate = LocalDate.of(2025, 9, 4);
        this.assetTradeWindow = AssetTradeWindow.builder()
                .symbol("BTCUSDT")
                .timeframe(Timeframe.FIFTEEN_MINUTES)
                .leverage(5)
                .build();
    }
}

package de.jansoh.rsistrategy.service.strategy.implementation;

import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;

import java.math.BigDecimal;

public class EmaCrossAdvancedStrategy implements AdvancedStrategy {

    private BarSeries series;
    private int ema20Length;
    private int ema50Length;
    private int ema200Length;
    private boolean useEma200Filter;
    private boolean useRsiFilter;
    private int rsiThreshold;
    private boolean useMacdFilter;
    private double macdThreshold;
    private int macdFastLength;
    private int macdSlowLength;
    private final boolean useVolumeFilter;
    private final double volMultiplier;
    private int volAvgPeriod;
    private final boolean useEma200DistanceFilter;
    private final double ema200DistPerc;
    private final boolean useEmaSlopeFilter;
    private final double emaSlopeThreshold;
    private final double tpUndercutPerc;
    private final double slMultiplier;
    private final boolean allowLong;
    private final boolean allowShort;

    private ClosePriceIndicator closePrice;
    private VolumeIndicator volume;
    private HighPriceIndicator highPrice;
    private LowPriceIndicator lowPrice;

    private EMAIndicator ema20;
    private EMAIndicator ema50;
    private EMAIndicator ema200;

    // --- Entry Signal: EMA 20 crossing EMA 50 ---
    private Rule ema20CrossAbove50;
    private Rule ema20CrossBelow50;

    // --- Filters ---
    // 1. EMA 200 Filter
    private Rule ema200LongMet;
    private Rule ema200ShortMet;

    // 2. RSI Filter
    private RSIIndicator rsi;
    private Rule rsiLongMet;
    private Rule rsiShortMet;

    // 3. MACD Filter
    private MACDIndicator macdLine;
    private Rule macdLongMet;
    private Rule macdShortMet;

    // 4. Volume Filter
    private SMAIndicator avgVolume;

    // Rules
    // volume > volMultiplier * avgVolume
    private Rule volumeMet;
    // 5. EMA 200 Distance Filter
    // |close - ema200| <= (ema200DistPerc / 100) * close
    // We can use a custom rule or transform indicators
    // Let's use a custom rule for simplicity in logic
    private Rule ema200DistMet;


    // 6. EMA Slope Filter
    // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
    private Rule emaSlopeLongMet;
    private Rule emaSlopeShortMet;

    // Exit strategy: EMA 20 with undercut
    // Long: Close < EMA 20 * (1 - d/100)
    // Short: Close > EMA 20 * (1 + d/100)
    private Rule longExitMet;
    private Rule shortExitMet;

    // --- Final Entry Conditions ---
    private Rule entryRuleLong;

    private Rule entryRuleShort;

    private int entryIndex;

    /**
     * Builds and returns a trading strategy based on various technical indicators
     * and conditions specified by the input parameters.
     * <p>
     * The strategy includes entry and exit rules for both long and short positions,
     * using EMA crossings, RSI, MACD, volume, and other filters for trade signals.
     * The parameters control whether each filter is applied and the thresholds for their application.
     *
     * @param series                  the time-series data (candlestick data) to base the strategy on
     * @param ema20Length             the window size for calculating the 20-period EMA
     * @param ema50Length             the window size for calculating the 50-period EMA
     * @param ema200Length            the window size for calculating the 200-period EMA
     * @param useEma200Filter         whether to use the EMA 200 filter for a trend direction
     * @param useRsiFilter            whether to include the RSI filter
     * @param rsiThreshold            the RSI threshold for filtering trades
     * @param useMacdFilter           whether to use the MACD filter
     * @param macdThreshold           the MACD threshold for filtering trades
     * @param macdFastLength          the window size for the fast period of MACD
     * @param macdSlowLength          the window size for the slow period of MACD
     * @param useVolumeFilter         whether to use a volume-based filter
     * @param volMultiplier           the multiplier for average volume used in the volume filter
     * @param volAvgPeriod            the window size for calculating the average volume
     * @param useEma200DistanceFilter whether to use a distance filter from EMA 200 for trade validation
     * @param ema200DistPerc          the maximum allowed percentage distance from EMA 200 to validate a trade
     * @param useEmaSlopeFilter       whether to use the slope of EMA for additional filtering
     * @param emaSlopeThreshold       the slope threshold for EMA filter in degrees (note that 1% price change = 180 degrees)
     * @param tpUndercutPerc          the percentage undercut for take-profit levels
     * @param slMultiplier            the multiplier for calculating stop-loss levels
     * @param allowLong               whether to allow long positions in the strategy
     * @param allowShort              whether to allow short positions in the strategy
     * @return a {@code Strategy} object representing the constructed trading strategy
     */
    public EmaCrossAdvancedStrategy(BarSeries series,
                                    int ema20Length, int ema50Length, int ema200Length,
                                    boolean useEma200Filter,
                                    boolean useRsiFilter, int rsiThreshold,
                                    boolean useMacdFilter, double macdThreshold, int macdFastLength, int macdSlowLength,
                                    boolean useVolumeFilter, double volMultiplier, int volAvgPeriod,
                                    boolean useEma200DistanceFilter,
                                    double ema200DistPerc,
                                    boolean useEmaSlopeFilter,
                                    double emaSlopeThreshold,
                                    double tpUndercutPerc,
                                    double slMultiplier,
                                    boolean allowLong,
                                    boolean allowShort) {

        this.series = series;
        this.ema20Length = ema20Length;
        this.ema50Length = ema50Length;
        this.ema200Length = ema200Length;
        this.useEma200Filter = useEma200Filter;
        this.useRsiFilter = useRsiFilter;
        this.rsiThreshold = rsiThreshold;
        this.useMacdFilter = useMacdFilter;
        this.macdThreshold = macdThreshold;
        this.macdFastLength = macdFastLength;
        this.macdSlowLength = macdSlowLength;
        this.useVolumeFilter = useVolumeFilter;
        this.volMultiplier = volMultiplier;
        this.volAvgPeriod = volAvgPeriod;
        this.useEma200DistanceFilter = useEma200DistanceFilter;
        this.ema200DistPerc = ema200DistPerc;
        this.useEmaSlopeFilter = useEmaSlopeFilter;
        this.emaSlopeThreshold = emaSlopeThreshold;
        this.tpUndercutPerc = tpUndercutPerc;
        this.slMultiplier = slMultiplier;
        this.allowLong = allowLong;
        this.allowShort = allowShort;

        initRules();
    }

    private void initRules() {

        this.closePrice = new ClosePriceIndicator(series);
        this.volume = new VolumeIndicator(series);
        this.highPrice = new HighPriceIndicator(series);
        this.lowPrice = new LowPriceIndicator(series);

        this.ema20 = new EMAIndicator(closePrice, ema20Length);
        this.ema50 = new EMAIndicator(closePrice, ema50Length);
        this.ema200 = new EMAIndicator(closePrice, ema200Length);

        // --- Entry Signal: EMA 20 crossing EMA 50 ---
        this.ema20CrossAbove50 = new CrossedUpIndicatorRule(ema20, ema50);
        this.ema20CrossBelow50 = new CrossedDownIndicatorRule(ema20, ema50);

        // --- Filters ---
        // 1. EMA 200 Filter
        this.ema200LongMet = useEma200Filter ? new OverIndicatorRule(closePrice, ema200) : new BooleanRule(true);
        this.ema200ShortMet = useEma200Filter ? new UnderIndicatorRule(closePrice, ema200) : new BooleanRule(true);

        // 2. RSI Filter
        this.rsi = new RSIIndicator(closePrice, 14);
        this.rsiLongMet = useRsiFilter ? new OverIndicatorRule(rsi, rsiThreshold) : new BooleanRule(true);
        this.rsiShortMet = useRsiFilter ? new UnderIndicatorRule(rsi, rsiThreshold) : new BooleanRule(true);

        // 3. MACD Filter
        this.macdLine = new MACDIndicator(closePrice, macdFastLength, macdSlowLength);
        this.macdLongMet = useMacdFilter ? new OverIndicatorRule(macdLine, macdThreshold) : new BooleanRule(true);
        this.macdShortMet = useMacdFilter ? new UnderIndicatorRule(macdLine, macdThreshold) : new BooleanRule(true);

        // 4. Volume Filter
        this.avgVolume = new SMAIndicator(volume, volAvgPeriod);

        // Rules
        // volume > volMultiplier * avgVolume
        this.volumeMet = new BooleanRule(true);
        // 5. EMA 200 Distance Filter
        // |close - ema200| <= (ema200DistPerc / 100) * close
        // We can use a custom rule or transform indicators
        // Let's use a custom rule for simplicity in logic
        this.ema200DistMet = new BooleanRule(true);

        // 6. EMA Slope Filter
        // g = atan((ema20 - ema20[1]) / ema20[1] * 100) * 180 / PI
        this.emaSlopeLongMet = new BooleanRule(true);
        this.emaSlopeShortMet = new BooleanRule(true);

        this.entryRuleLong = new BooleanRule(false);
        this.entryRuleShort = new BooleanRule(false);
        this.entryIndex = 0;

        if (useEmaSlopeFilter) {
            emaSlopeLongMet = (index, tradingRecord) -> {
                if (index < 1) return false;
                double angle = calculateAngle(index, ema20);
                return angle >= emaSlopeThreshold;
            };
            emaSlopeShortMet = (index, tradingRecord) -> {
                if (index < 1) return false;
                double angle = calculateAngle(index, ema20);
                return angle <= -emaSlopeThreshold;
            };
        }

        if (useVolumeFilter) {
            volumeMet = (index, tradingRecord) -> {
                Num vol = volume.getValue(index);
                Num avg = avgVolume.getValue(index);
                return vol.isGreaterThan(avg.multipliedBy(series.numOf(volMultiplier)));
            };
        }

        if (useEma200DistanceFilter) {
            ema200DistMet = (index, tradingRecord) -> {
                Num close = closePrice.getValue(index);
                Num ema = ema200.getValue(index);
                Num dist = close.minus(ema).abs();
                Num maxDist = close.multipliedBy(series.numOf(ema200DistPerc)).dividedBy(series.numOf(100));
                return dist.isLessThanOrEqual(maxDist);
            };
        }

        if (allowLong) {
            entryRuleLong = ema200LongMet
                    .and(rsiLongMet)
                    .and(macdLongMet)
                    .and(volumeMet)
                    .and(ema200DistMet)
                    .and(emaSlopeLongMet)
                    .and(ema20CrossAbove50);
        }

        if (allowShort) {
            entryRuleShort = ema200ShortMet
                    .and(rsiShortMet)
                    .and(macdShortMet)
                    .and(volumeMet)
                    .and(ema200DistMet)
                    .and(emaSlopeShortMet)
                    .and(ema20CrossBelow50);
        }


        longExitMet = (index, tradingRecord) -> {
            Num close = closePrice.getValue(index);
            Num ema = ema20.getValue(index);
            Num undercutFactor = series.numOf(1).minus(series.numOf(tpUndercutPerc).dividedBy(series.numOf(100)));
            boolean emaExit = close.isLessThan(ema.multipliedBy(undercutFactor));

            // Candle after entry condition
            boolean pHeightExit = false;
            if (tradingRecord != null && tradingRecord.getCurrentPosition().isOpened()) {
                int entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
                int candleAfterEntryIndex = entryIndex + 1;
                if (index > candleAfterEntryIndex && candleAfterEntryIndex < series.getBarCount()) {
                    Num p = closePrice.getValue(candleAfterEntryIndex);
                    Num h = highPrice.getValue(candleAfterEntryIndex).minus(lowPrice.getValue(candleAfterEntryIndex));
                    Num limit = p.minus(h.multipliedBy(series.numOf(slMultiplier)));
                    pHeightExit = close.isLessThan(limit);
                }
            }

            return emaExit || pHeightExit;
        };

        shortExitMet = new Rule() {
            @Override
            public boolean isSatisfied(int index, org.ta4j.core.TradingRecord tradingRecord) {
                Num close = closePrice.getValue(index);
                Num ema = ema20.getValue(index);
                Num undercutFactor = series.numOf(1).plus(series.numOf(tpUndercutPerc).dividedBy(series.numOf(100)));
                boolean emaExit = close.isGreaterThan(ema.multipliedBy(undercutFactor));

                // Candle after entry condition
                boolean pHeightExit = false;
                if (tradingRecord != null && tradingRecord.getCurrentPosition().isOpened()) {
                    entryIndex = tradingRecord.getCurrentPosition().getEntry().getIndex();
                    int candleAfterEntryIndex = entryIndex + 1;
                    if (index > candleAfterEntryIndex && candleAfterEntryIndex < series.getBarCount()) {
                        Num p = closePrice.getValue(candleAfterEntryIndex);
                        Num h = highPrice.getValue(candleAfterEntryIndex).minus(lowPrice.getValue(candleAfterEntryIndex));
                        Num limit = p.plus(h.multipliedBy(series.numOf(slMultiplier)));
                        pHeightExit = close.isGreaterThan(limit);
                    }
                }

                return emaExit || pHeightExit;
            }
        };
    }

    public Strategy getStrategy() {
        return new BaseStrategy("EMA 20/50 Advanced Strategy",
                entryRuleLong.or(entryRuleShort),
                longExitMet.or(shortExitMet));
    }

    private static double calculateAngle(int index, EMAIndicator ema20) {
        Num current = ema20.getValue(index);
        Num previous = ema20.getValue(index - 1);
        double slope = current.minus(previous).dividedBy(previous).multipliedBy(ema20.getBarSeries().numOf(100)).doubleValue();
        return Math.atan(slope) * 180.0 / Math.PI;
    }


    @Override
    public BigDecimal getSl(Bar positionEntry, Position position) {

        Num height = positionEntry.getClosePrice()
                .minus(positionEntry.getOpenPrice())
                .multipliedBy(DecimalNum.valueOf(slMultiplier))
                .abs();

        if (PositionSide.LONG == position.getSide()) {
            return new BigDecimal(positionEntry.getOpenPrice().minus(height).toString());
        }
        return new BigDecimal(positionEntry.getOpenPrice().plus(height).toString());
    }


    /**
     * Calculates the take-profit (TP) level for the given position based on its side.
     *
     * @param positionEntry the entry bar of the position, containing market data at the time of entry
     * @param position      the position for which the take-profit level is to be calculated
     * @return the take-profit level as a {@code BigDecimal}; returns {@code Long.MAX_VALUE} for long positions
     * and {@code BigDecimal.ZERO} for non-long positions - this strategy uses conditional TP levels.
     */
    @Override
    public BigDecimal getTp(Bar positionEntry, Position position) {
        if (PositionSide.LONG == position.getSide()) {
            return new BigDecimal(Long.MAX_VALUE);
        }
        return BigDecimal.ZERO;
    }
}

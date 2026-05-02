package de.jansoh.rsistrategy.service.indicator;

import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;

/**
 * Factory class for creating instances of {@code ATRIndicator}.
 * <p>
 * This class provides methods to create ATR (Average True Range) indicators used
 * for technical analysis in financial trading. The {@code ATRIndicator} calculates
 * the average range of price movements over a specified period.
 * <p>
 * The factory supports creating indicators with both a custom and a default time frame.
 * <p>
 * Marked as a Spring service to enable dependency injection, allowing this factory to
 * be managed and utilized as a Spring-managed bean.
 */
@Service
public class AtrIndicatorFactory {

    /**
     * Creates an instance of {@code ATRIndicator} with a specified {@code BarSeries}
     * and a fixed time frame of 4 bars.
     *
     * @param barSeries the {@code BarSeries} containing the data series to calculate the ATR.
     * @return an {@code ATRIndicator} instance configured with the provided {@code BarSeries}
     * and a time frame of 4 bars.
     */
    public ATRIndicator create(BarSeries barSeries) {
        return new ATRIndicator(barSeries, 4);
    }

    /**
     * Creates an instance of {@code ATRIndicator} with a default time frame of 14 bars.
     *
     * @param barSeries the {@code BarSeries} containing the data series to calculate the ATR.
     * @return an {@code ATRIndicator} instance configured with the provided {@code BarSeries}
     * and a default time frame of 14 bars.
     */
    public ATRIndicator createDefault(BarSeries barSeries) {
        return new ATRIndicator(barSeries, 14);
    }
}

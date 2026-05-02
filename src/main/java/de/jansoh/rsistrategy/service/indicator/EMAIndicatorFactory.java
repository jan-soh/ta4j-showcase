package de.jansoh.rsistrategy.service.indicator;

import org.springframework.stereotype.Component;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.num.Num;

/**
 * Factory class for creating instances of {@code EMAIndicator}.
 * <p>
 * This class provides a method to create Exponential Moving Average (EMA) indicators
 * used for analyzing financial data in the context of technical analysis. The
 * {@code EMAIndicator} computes a smoothed moving average where more weight is given
 * to recent values.
 * <p>
 * It is designed to be used as a Spring component, enabling dependency injection
 * and allowing this factory to be managed as a Spring-managed bean.
 */
@Component
public class EMAIndicatorFactory {

    /**
     * Creates an instance of {@code EMAIndicator} with the specified underlying {@code Indicator}
     * and the given bar count for calculating the Exponential Moving Average (EMA).
     *
     * @param indicator the underlying {@code Indicator<Num>} that provides the input data for the EMA calculation.
     * @param barCount  the number of bars (time period) to be used in the EMA calculation.
     * @return an {@code EMAIndicator} instance configured with the specified {@code Indicator} and bar count.
     */
    public EMAIndicator createEMAIndicator(Indicator<Num> indicator, int barCount) {
        return new EMAIndicator(indicator, barCount);
    }
}

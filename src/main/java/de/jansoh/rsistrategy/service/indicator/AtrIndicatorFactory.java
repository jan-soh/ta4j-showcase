package de.jansoh.rsistrategy.service.indicator;

import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;

@Service
public class AtrIndicatorFactory {

    public ATRIndicator create(BarSeries barSeries) {
        return new ATRIndicator(barSeries, 14);
    }
}

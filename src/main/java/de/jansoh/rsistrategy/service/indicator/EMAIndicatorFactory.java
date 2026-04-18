package de.jansoh.rsistrategy.service.indicator;

import org.springframework.stereotype.Component;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.num.Num;

@Component
public class EMAIndicatorFactory {

    public EMAIndicator createEMAIndicator(Indicator<Num> indicator, int barCount) {
        return new EMAIndicator(indicator, barCount);
    }
}

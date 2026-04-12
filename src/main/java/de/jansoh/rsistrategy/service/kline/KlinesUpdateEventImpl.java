package de.jansoh.rsistrategy.service.kline;

import de.jansoh.rsistrategy.model.Timeframe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.ta4j.core.BarSeries;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlinesUpdateEventImpl implements KlinesUpdateEvent {

    private String symbol;
    private Timeframe timeframe;
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

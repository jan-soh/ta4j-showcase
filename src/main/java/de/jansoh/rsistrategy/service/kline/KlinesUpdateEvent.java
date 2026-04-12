package de.jansoh.rsistrategy.service.kline;

import de.jansoh.rsistrategy.model.Timeframe;
import org.ta4j.core.BarSeries;

public interface KlinesUpdateEvent {

    String getSymbol();

    Timeframe getTimeframe();

    BarSeries getBarSeries();
}

package de.jansoh.rsistrategy.ta4jbridge.model;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTrade;

public class TsTrade extends BaseTrade {

    public TsTrade(int index, BarSeries series, TradeType type) {
        super(index, series, type);
    }
}

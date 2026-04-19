package de.jansoh.rsistrategy.ta4jbridge.model;

import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Position;
import org.ta4j.core.Trade;

public class TsTradingRecord extends BaseTradingRecord {

    public TsTradingRecord(Position position) {
        super(position);
    }

    public TsTradingRecord(String name, Trade.TradeType tradeType) {
        super(name, tradeType);
    }
}

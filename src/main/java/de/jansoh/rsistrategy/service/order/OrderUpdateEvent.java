package de.jansoh.rsistrategy.service.order;

import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.model.Timeframe;

public interface OrderUpdateEvent {

    String getSymbol();

    Timeframe getTimeframe();

    Order getOrder();
}

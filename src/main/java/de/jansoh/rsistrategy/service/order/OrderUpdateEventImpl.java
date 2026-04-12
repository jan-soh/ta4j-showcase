package de.jansoh.rsistrategy.service.order;

import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.model.Timeframe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateEventImpl implements OrderUpdateEvent {

    private String symbol;
    private Timeframe timeframe;
    private Order order;

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public Timeframe getTimeframe() {
        return timeframe;
    }

    @Override
    public Order getOrder() {
        return order;
    }
}

package de.jansoh.rsistrategy.service.order;

import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.model.Timeframe;

/**
 * Represents an event that provides updates related to a specific order.
 * This interface is designed to encapsulate information about an updated order,
 * including the trading symbol, associated timeframe, and the order itself.
 */
public interface OrderUpdateEvent {

    /**
     * Retrieves the trading symbol associated with this order update event.
     *
     * @return the trading symbol as a String
     */
    String getSymbol();

    /**
     * Retrieves the timeframe associated with this order update event.
     * The timeframe provides a standardized interval that describes the
     * duration or period related to the data or updates represented in
     * the event.
     *
     * @return the {@code Timeframe} associated with this order update event
     */
    Timeframe getTimeframe();

    /**
     * Retrieves the order associated with this order update event.
     *
     * @return the {@code Order} associated with this event
     */
    Order getOrder();
}

package de.jansoh.rsistrategy.service.order;

import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.model.Timeframe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Implementation of the {@code OrderUpdateEvent} interface, representing an event
 * that provides updates related to a specific order. This class encapsulates
 * information such as the trading symbol, the associated timeframe, and the order
 * itself.
 * <p>
 * Instances of this class serve as concrete representations of order update events,
 * carrying the relevant data for further processing or handling.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateEventImpl implements OrderUpdateEvent {

    /**
     * The trading symbol associated with the order update event.
     * Represents the unique identifier or ticker symbol of the financial
     * instrument involved in the order.
     */
    private String symbol;

    /**
     * The timeframe associated with the order update event.
     * Represents a predefined time interval that serves as a standardized
     * reference for the duration or period related to the event's data
     * or updates.
     */
    private Timeframe timeframe;
    private Order order;

    /**
     * Retrieves the trading symbol associated with this order update event.
     * This symbol represents the unique identifier or ticker symbol
     * of the financial instrument related to the order.
     *
     * @return the trading symbol as a String
     */
    @Override
    public String getSymbol() {
        return symbol;
    }

    /**
     * Retrieves the timeframe associated with this order update event.
     * The timeframe represents a standardized time interval that provides
     * a reference for the duration or period related to the event's data
     * or updates.
     *
     * @return the {@code Timeframe} associated with this order update event
     */
    @Override
    public Timeframe getTimeframe() {
        return timeframe;
    }

    /**
     * Retrieves the order associated with this order update event.
     * The order represents the specific transaction or trading instruction
     * related to this event.
     *
     * @return the {@code Order} associated with this order update event
     */
    @Override
    public Order getOrder() {
        return order;
    }
}

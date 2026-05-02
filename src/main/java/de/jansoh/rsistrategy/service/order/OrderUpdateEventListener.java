package de.jansoh.rsistrategy.service.order;

/**
 * Defines a listener interface for handling {@link OrderUpdateEvent}.
 * Implementations of this interface are intended to respond to updates
 * related to orders, typically in trading or financial applications.
 * <p>
 * Listeners can use the provided {@link OrderUpdateEvent} to access details
 * such as the updated order, trading symbol, and associated timeframe.
 * This interface can be implemented to define custom logic for processing
 * these updates as part of the application's functionality.
 */
public interface OrderUpdateEventListener {

    /**
     * Handles updates related to an order. This method is invoked when an
     * {@link OrderUpdateEvent} occurs, allowing implementations to react to changes
     * in order status, details, or other event-specific information.
     *
     * @param event the event containing details about the updated order,
     *              including the trading symbol, associated timeframe,
     *              and the updated order itself
     */
    void onOrderUpdate(OrderUpdateEvent event);
}

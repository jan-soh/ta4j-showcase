package de.jansoh.rsistrategy.model;

/**
 * Represents the status of an order.
 * <p>
 * The available statuses are:
 * - {@code NEW}: Indicates that the order is newly created and not yet processed.
 * - {@code FILLED}: Indicates that the order has been fully processed.
 * - {@code PARTIALLY_FILLED}: Indicates that the order has been partially processed but not fully completed.
 */
public enum OrderStatus {
    NEW,
    FILLED,
    PARTIALLY_FILLED;

    /**
     * Checks if the current enum instance matches any of the provided {@code OrderStatus} values.
     *
     * @param orderStatuses an array of {@code OrderStatus} values to compare against
     * @return {@code true} if the current instance is one of the provided statuses, {@code false} otherwise
     */
    public boolean isOneOf(OrderStatus... orderStatuses) {
        for (OrderStatus orderStatus : orderStatuses) {
            if (this == orderStatus) return true;
        }
        return false;
    }
}

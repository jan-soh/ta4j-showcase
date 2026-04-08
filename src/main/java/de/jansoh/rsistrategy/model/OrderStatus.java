package de.jansoh.rsistrategy.model;

public enum OrderStatus {
    NEW,
    FILLED,
    PARTIALLY_FILLED;

    public boolean isOneOf(OrderStatus... orderStatuses) {
        for (OrderStatus orderStatus : orderStatuses) {
            if (this == orderStatus) return true;
        }
        return false;
    }
}

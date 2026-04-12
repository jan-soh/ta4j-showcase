package de.jansoh.rsistrategy.service.order;

public interface OrderUpdateEventListener {
    void onOrderUpdate(OrderUpdateEvent event);
}

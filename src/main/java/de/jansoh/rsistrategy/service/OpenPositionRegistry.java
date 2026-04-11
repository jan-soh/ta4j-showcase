package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class OpenPositionRegistry {

    private final Map<String, Position> openPositionsById = new HashMap<>();
    private final Map<Integer, Position> openPositionsByQty = new HashMap<>();

    public void add(Position position) {
        openPositionsById.put(position.getOrderId(), position);
        openPositionsByQty.put(hash(position), position);
    }

    private int hash(Position position) {
        return Objects.hash(position.getSymbol(), position.getQuantity().toPlainString());
    }

    private int hash(Order order) {
        return Objects.hash(order.getSymbol(), order.getOriginalQuantity().toPlainString());
    }

    private int hash(AlgoOrder algoOrder) {
        return Objects.hash(algoOrder.getSymbol(), algoOrder.getQuantity().toPlainString());
    }

    public Position get(String orderId) {
        return openPositionsById.get(orderId);
    }

    public Optional<Position> update(AlgoOrder algoOrder) {
        Position position = openPositionsByQty.get(hash(algoOrder));
        if (null == position) {
            throw new OpenPositionRegistrationException("Position with symbol " + algoOrder.getSymbol() + " and quantity "
                    + algoOrder.getQuantity().toPlainString() + " not found for algo order: " + algoOrder.getAlgoId());
        }

        if (OrderSide.SELL.equals(algoOrder.getSide())) {

            position.setTpAlgoOrderId(algoOrder.getAlgoId());
            position.setTpAlgoPrice(algoOrder.getTriggerPrice());
        } else {

            position.setSlAlgoOrderId(algoOrder.getAlgoId());
            position.setSlAlgoPrice(algoOrder.getTriggerPrice());
        }

        return Optional.of(position);
    }

    public Optional<Position> update(Order order) {

        if (!(OrderStatus.PARTIALLY_FILLED.equals(order.getOrderStatus()) || OrderStatus.FILLED.equals(order.getOrderStatus()))) {
            // update only for PARTIALLY_FILLED and FILLED orders
            return Optional.empty();
        }

        String orderId = order.getOrderId();
        int hash = hash(order);

        Position position;

        // If the order ID refers to a known position, the position is still built up (with PARTIALLY_FILLED orders)
        if (openPositionsById.containsKey(orderId)) {
            position = openPositionsById.get(orderId);
            position.setSide(order.getSide() == OrderSide.BUY ? PositionSide.LONG : PositionSide.SHORT);
            position.setOpenTime(order.getOrderTradeTime());
            position.setAverageOpenPrice(order.getLastFilledPrice());
            position.setRealizedProfit(position.getRealizedProfit().add(order.getRealizedProfit()));
            // If the position cannot be identified by order ID but matches the order's symbol and original quantity,
            // this order is most likely an algo order triggered by SL or TP.
        } else if (openPositionsByQty.containsKey(hash)) {
            position = openPositionsByQty.get(hash);

            // close the position if the order is fully filled
            if (OrderStatus.FILLED.equals(order.getOrderStatus())) {
                position.setClosedTime(order.getOrderTradeTime());
                position.setAverageClosedPrice(order.getLastFilledPrice());
                position.setClosed(true);
            }
        } else {

            // Because we don't know the order ID yet, add a new position.
            position = Position.builder()
                    .orderId(orderId)
                    .symbol(order.getSymbol())
                    .quantity(order.getOriginalQuantity())
                    .side(order.getSide() == OrderSide.BUY ? PositionSide.LONG : PositionSide.SHORT)
                    .openTime(order.getOrderTradeTime())
                    .averageOpenPrice(order.getLastFilledPrice())
                    .realizedProfit(BigDecimal.ZERO)
                    .build();
            openPositionsById.put(orderId, position);
            openPositionsByQty.put(hash, position);
        }

        position.setRealizedProfit(position.getRealizedProfit().add(order.getRealizedProfit()));

        if (position.isClosed()) {
            openPositionsById.remove(orderId);
            openPositionsByQty.remove(hash);
        }

        return Optional.of(position);
    }
}

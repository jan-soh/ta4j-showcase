package de.jansoh.rsistrategy.service.position;

import de.jansoh.rsistrategy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
public class OpenPositionRegistry {

    private final Map<String, Position> openPositionsById = new HashMap<>();
    private final Map<String, Position> openPositionsByClientId = new HashMap<>();
    private final Map<AssetTradeWindow, List<Position>> openPositionsByTradeWindow = new HashMap<>();

    public void update(Position position) {

        if (openPositionsById.containsKey(position.getOrderId())) {
            Position positionExisting = openPositionsById.get(position.getOrderId());
            positionExisting.setTimeframe(position.getTimeframe());
        } else {
            openPositionsById.put(position.getOrderId(), position);
        }

        AssetTradeWindow atw = AssetTradeWindow.builder()
                .symbol(position.getSymbol())
                .timeframe(position.getTimeframe())
                .build();

        if (openPositionsByTradeWindow.containsKey(atw)) {
            List<Position> positions = openPositionsByTradeWindow.get(atw);
            positions.add(position);
        } else {
            List<Position> positions = new ArrayList<>();
            positions.add(position);
            openPositionsByTradeWindow.put(atw, positions);
        }
    }

    public Position get(String orderId) {
        return openPositionsById.get(orderId);
    }

    public Optional<Position> update(AlgoOrder algoOrder) {

        Position position = openPositionsById.get(algoOrder.getOrderId());

        if (null == position) {
            throw new OpenPositionRegistrationException("Position with symbol " + algoOrder.getSymbol() + " and quantity "
                    + algoOrder.getQuantity().toPlainString() + " not found for algo order: " + algoOrder.getAlgoId());
        }

        if (AlgoOrderType.TP.equals(algoOrder.getType())) {

            position.setTpAlgoOrderId(algoOrder.getAlgoId());
            position.setTpAlgoPrice(algoOrder.getTriggerPrice());
            position.setTpClientOrderId(algoOrder.getClientOrderId());
        } else {

            position.setSlAlgoOrderId(algoOrder.getAlgoId());
            position.setSlAlgoPrice(algoOrder.getTriggerPrice());
            position.setTpClientOrderId(algoOrder.getClientOrderId());
        }

        openPositionsByClientId.put(algoOrder.getClientOrderId(), position);

        return Optional.of(position);
    }

    public synchronized Optional<Position> update(Order order) {

        if (!(OrderStatus.PARTIALLY_FILLED.equals(order.getOrderStatus()) || OrderStatus.FILLED.equals(order.getOrderStatus()))) {
            // update only for PARTIALLY_FILLED and FILLED orders
            return Optional.empty();
        }

        String orderId = order.getOrderId();
        String clientOrderId = order.getClientOrderId();

        Position position;

        // If the order ID refers to a known position, the position is still built up (with PARTIALLY_FILLED orders)
        if (openPositionsById.containsKey(orderId)) {

            position = openPositionsById.get(orderId);

            // updating a known position
            position.setOpenTime(order.getOrderTradeTime());
            position.setAverageOpenPrice(order.getLastFilledPrice());

            log.info("----- OPEN_POSITION_REGISTRY ----- updating existing position for {} with order state {}.", order.getSymbol(), order.getOrderStatus());
        } else if (openPositionsByClientId.containsKey(clientOrderId)) {

            // If the client order ID is know here, it refers to a TP/SL algo order. In this case, the position will be closed, due to TP/SL trigger.
            position = openPositionsByClientId.get(clientOrderId);

            // closing due to TP/SL trigger
            position.setClosedTime(order.getOrderTradeTime());
            position.setAverageClosedPrice(order.getLastFilledPrice());

            log.info("----- OPEN_POSITION_REGISTRY ----- TP/SL ({}) for existing position in {} filled with order state {}.", order.getClientOrderId(), order.getSymbol(), order.getOrderStatus());

            if (OrderStatus.FILLED.equals(order.getOrderStatus())) {
                position.setClosed(true);
                log.info("----- OPEN_POSITION_REGISTRY ----- existing position in {} marked closed.", order.getSymbol());
            }
        } else {

            // Because we don't know the order ID yet, add a new position
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

            log.info("----- OPEN_POSITION_REGISTRY ----- new position created for {} and size {}.", order.getSymbol(), order.getOriginalQuantity());
        }

        position.addRealizedProfit(order.getRealizedProfit());

        if (position.isClosed()) {
            openPositionsById.remove(orderId);
            openPositionsByClientId.remove(position.getTpClientOrderId());
            openPositionsByClientId.remove(position.getSlClientOrderId());
            AssetTradeWindow atw = AssetTradeWindow.builder()
                    .symbol(position.getSymbol())
                    .timeframe(position.getTimeframe())
                    .build();
            List<Position> positions = openPositionsByTradeWindow.get(atw);
            List<Position> newPositions = new ArrayList<>();
            positions.forEach(p -> {
                if (!p.getOrderId().equals(orderId)) {
                    newPositions.add(p);
                }
            });
            openPositionsByTradeWindow.put(atw, newPositions);
        }

        return Optional.of(position);
    }

    public synchronized boolean hasPositions(AssetTradeWindow atw) {
        return openPositionsByTradeWindow.containsKey(atw) && !openPositionsByTradeWindow.get(atw).isEmpty();
    }

    public synchronized List<Position> getPositions(AssetTradeWindow atw) {
        return openPositionsByTradeWindow.get(atw);
    }
}

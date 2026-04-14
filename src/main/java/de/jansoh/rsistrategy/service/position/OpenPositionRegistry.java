package de.jansoh.rsistrategy.service.position;

import de.jansoh.rsistrategy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                    + algoOrder.getQuantity().setScale(4, RoundingMode.HALF_UP).toPlainString() + " not found for algo order: " + algoOrder.getAlgoId());
        }

        if (AlgoOrderType.TP.equals(algoOrder.getType())) {

            position.setTpAlgoId(algoOrder.getAlgoId());
            position.setTpAlgoPrice(algoOrder.getTriggerPrice());
            position.setTpClientOrderId(algoOrder.getClientOrderId());
        } else {

            position.setSlAlgoId(algoOrder.getAlgoId());
            position.setSlAlgoPrice(algoOrder.getTriggerPrice());
            position.setSlClientOrderId(algoOrder.getClientOrderId());
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

        // if this order closes an existing position, then the order has a client order ID starting with "close_"
        if (clientOrderId.startsWith("close_")) {
            orderId = clientOrderId.substring(6);
            position = openPositionsById.get(orderId);

            if (null == position) {
                throw new OpenPositionRegistrationException("No position to close found with order ID: " + orderId);
            }

            position.setAverageClosedPrice(order.getLastFilledPrice());
            position.setClosedTime(order.getOrderTradeTime());

            if (OrderStatus.FILLED.equals(order.getOrderStatus())) {
                log.info("----- OPEN_POSITION_REGISTRY ----- position {}, was force closed by order {} before TP/SL mark. Marking position as closed.", position.getOrderId(), order.getOrderId());
                position.setTpOrderFilled(false);
                position.setSlOrderFilled(false);
                position.setClosed(true);
            }
            // If the order ID refers to a known position, the position is still built up (with PARTIALLY_FILLED orders)
        } else if (openPositionsById.containsKey(orderId)) {

            position = openPositionsById.get(orderId);

            // updating a known position
            position.setOpenTime(order.getOrderTradeTime());
            position.setAverageOpenPrice(order.getLastFilledPrice());

            log.info("----- OPEN_POSITION_REGISTRY ----- updating position {} with order {} and state {}.", position.getOrderId(), order.getOrderId(), order.getOrderStatus());
        } else if (openPositionsByClientId.containsKey(clientOrderId)) {

            // If the client order ID is know here, it refers to a TP/SL algo order. In this case, the position will be closed, due to TP/SL trigger.
            position = openPositionsByClientId.get(clientOrderId);

            // closing due to TP/SL trigger
            position.setClosedTime(order.getOrderTradeTime());
            position.setAverageClosedPrice(order.getLastFilledPrice());

            if (OrderStatus.FILLED.equals(order.getOrderStatus())) {

                position.setClosed(true);

                if (clientOrderId.startsWith("algo_sl_")) {
                    log.info("----- OPEN_POSITION_REGISTRY ----- SL for position {} filled by order {}.", position.getOrderId(), order.getOrderId());
                    position.setSlOrderFilled(true);
                } else if (clientOrderId.startsWith("algo_tp_")) {
                    log.info("----- OPEN_POSITION_REGISTRY ----- TP for position {} filled by order {}.", position.getOrderId(), order.getOrderId());
                    position.setTpOrderFilled(true);
                }

                log.info("----- OPEN_POSITION_REGISTRY ----- position {} marked closed.", position.getOrderId());
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

            log.info("----- OPEN_POSITION_REGISTRY ----- new {} position created for {} and size {}.", position.getSide(), order.getSymbol(), order.getOriginalQuantity().setScale(4, RoundingMode.HALF_UP));
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
            final String oid = orderId;
            positions.forEach(p -> {
                if (!p.getOrderId().equals(oid)) {
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

package de.jansoh.rsistrategy.service.position;

import de.jansoh.rsistrategy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * The OpenPositionRegistry class is responsible for managing and tracking open positions
 * within a trading system. It maintains a registry of positions organized by order IDs,
 * client order IDs, and asset trade windows. This class provides methods to update, retrieve,
 * and close positions based on execution details from orders or algorithmic trading events.
 * <p>
 * The registry supports the following functionalities:
 * - Adding or updating open positions based on order or algorithmic order data.
 * - Tracking positions associated with specific trade windows (symbol and timeframe).
 * - Handling modifications and closures of existing positions, including partial and complete closures.
 * - Managing realized profits and marking positions as closed when trades are completed.
 */
@Slf4j
@Component
public class OpenPositionRegistry {

    /**
     * A mapping of position identifiers to their associated {@code Position} objects.
     * This map is used to track currently open positions by their unique identifiers.
     * <p>
     * Key: A {@code String} representing the unique identifier of a position (e.g., order ID).
     * Value: A {@code Position} representing the information associated with the open position.
     */
    private final Map<String, Position> openPositionsById = new HashMap<>();

    /**
     * Represents a mapping of client identifiers to their associated open positions.
     * This is used to track and manage the current open positions for each client
     * in the system.
     * <p>
     * Key: The unique identifier for a client (String).
     * Value: The corresponding open position associated with the client (Position).
     * <p>
     * This map facilitates querying, updating, and managing open positions
     * by client identifier.
     * <p>
     * It is part of the OpenPositionRegistry class for handling position-related operations.
     */
    private final Map<String, Position> openPositionsByClientId = new HashMap<>();

    /**
     * A mapping that associates a specific {@link AssetTradeWindow} to a list of
     * {@link Position} objects representing the open positions for that trade window.
     * <p>
     * This map serves as a registry to track active positions grouped by their
     * respective asset trading windows. The key is an {@link AssetTradeWindow} object,
     * which defines the trading configuration such as symbol, timeframe, and leverage.
     * The value is a list of {@link Position} objects representing the active trades
     * associated with the given trade window.
     * <p>
     * Key Characteristics:
     * - Enables retrieval of positions specific to an asset trade window.
     * - Supports synchronization and concurrency for operations involving
     * trade window position management.
     * - Facilitates operations like updating, checking for open positions,
     * and retrieving all positions for a given trade window.
     * <p>
     * For example, this structure allows efficient queries to determine if any
     * positions exist for a specific {@link AssetTradeWindow}, retrieve all
     * positions for a trade window, or update the positions as changes occur.
     * <p>
     * Thread Safety:
     * - Access to this map is synchronized through the methods in the
     * {@code OpenPositionRegistry} class to ensure thread safety when
     * interacting with open positions.
     */
    private final Map<AssetTradeWindow, List<Position>> openPositionsByTradeWindow = new HashMap<>();

    /**
     * Updates the position information in the registry. If a position with the same order ID
     * already exists, its timeframe is updated. Otherwise, the new position is added to the
     * registry. Additionally, the position is associated with the corresponding asset trade window.
     *
     * @param position the position object containing the orderId, symbol, timeframe, and other relevant information
     */
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

    /**
     * Retrieves the position associated with the specified order ID from the registry.
     *
     * @param orderId the unique identifier of the order whose position is to be retrieved
     * @return the position associated with the order ID, or null if no position exists for the given ID
     */
    public Position get(String orderId) {
        return openPositionsById.get(orderId);
    }

    /**
     * Updates an existing position in the registry based on the provided algorithmic order.
     * If the position corresponding to the order ID does not exist, an exception is thrown.
     * The method determines whether the order is a take-profit (TP) or stop-loss (SL) type
     * and updates the respective attributes of the position accordingly.
     *
     * @param algoOrder the algorithmic order containing details such as order ID, symbol,
     *                  quantity, trigger price, client order ID, and order type
     * @return an {@code Optional} containing the updated {@code Position} if successfully updated
     * @throws OpenPositionRegistrationException if no position exists for the given order ID
     */
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

    /**
     * Updates the position information in the registry based on the provided order.
     * The method determines whether the order corresponds to opening a new position,
     * updating an existing position, or closing a position (either due to take-profit (TP)
     * or stop-loss (SL) triggers). Positions associated with the given order ID or
     * client order ID are updated accordingly. If the position is closed, it is removed
     * from the corresponding tracking structures.
     *
     * @param order the order object containing details such as order ID, client order ID,
     *              symbol, quantity, order status, side, trade time, last filled price,
     *              and realized profit
     * @return an {@code Optional} containing the updated {@code Position} if successfully updated,
     * or {@code Optional.empty()} if the order does not meet the conditions for updates
     * @throws OpenPositionRegistrationException if no matching position exists to close for
     *                                           the given client order ID with a "close_" prefix
     */
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
                    .timeframe(order.getTimeframe())
                    .quantity(order.getOriginalQuantity())
                    .side(order.getSide() == OrderSide.BUY ? PositionSide.LONG : PositionSide.SHORT)
                    .openTime(order.getOrderTradeTime())
                    .averageOpenPrice(order.getLastFilledPrice())
                    .realizedProfit(BigDecimal.ZERO)
                    .build();

            openPositionsById.put(orderId, position);

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

            log.info("----- OPEN_POSITION_REGISTRY ----- new {} position created for {} and size {}.", position.getSide(), order.getSymbol(), order.getOriginalQuantity().setScale(4, RoundingMode.HALF_UP));
        }

        position.addRealizedProfit(order.getRealizedProfit());

        if (position.isClosed()) {
            openPositionsById.remove(position.getOrderId());
            openPositionsByClientId.remove(position.getTpClientOrderId());
            openPositionsByClientId.remove(position.getSlClientOrderId());
            AssetTradeWindow atw = AssetTradeWindow.builder()
                    .symbol(position.getSymbol())
                    .timeframe(position.getTimeframe())
                    .build();
            List<Position> positions = openPositionsByTradeWindow.get(atw);
            List<Position> newPositions = new ArrayList<>();
            final String oid = position.getOrderId();
            positions.forEach(p -> {
                if (!p.getOrderId().equals(oid)) {
                    newPositions.add(p);
                }
            });
            openPositionsByTradeWindow.put(atw, newPositions);
        }

        return Optional.of(position);
    }

    /**
     * Checks whether there are any open positions associated with the given asset trade window.
     * This method examines the internal mapping of open positions and determines whether
     * the specified trade window has any corresponding positions recorded in the registry.
     *
     * @param atw the {@code AssetTradeWindow} object representing the trading window for which positions are to be checked
     * @return {@code true} if there are one or more open positions associated with the specified trade window;
     * {@code false} otherwise
     */
    public synchronized boolean hasPositions(AssetTradeWindow atw) {
        return openPositionsByTradeWindow.containsKey(atw) && !openPositionsByTradeWindow.get(atw).isEmpty();
    }

    /**
     * Retrieves the list of open positions associated with the specified asset trade window.
     *
     * @param atw the {@code AssetTradeWindow} object representing the trading window
     *            for which the open positions are to be retrieved
     * @return a {@code List} of {@code Position} objects associated with the specified
     * asset trade window, or {@code null} if no positions are mapped to the given trade window
     */
    public synchronized List<Position> getPositions(AssetTradeWindow atw) {
        return openPositionsByTradeWindow.get(atw);
    }
}

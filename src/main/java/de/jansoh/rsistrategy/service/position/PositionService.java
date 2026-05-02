package de.jansoh.rsistrategy.service.position;

import de.jansoh.rsistrategy.model.*;
import de.jansoh.rsistrategy.repository.OrderRepository;
import de.jansoh.rsistrategy.repository.PositionRepository;
import de.jansoh.rsistrategy.service.BinanceApiService;
import de.jansoh.rsistrategy.service.BinanceApiServiceOrderException;
import de.jansoh.rsistrategy.service.MessageService;
import de.jansoh.rsistrategy.service.PrecisionService;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProvider;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProviderFactory;
import de.jansoh.rsistrategy.service.order.OrderUpdateEvent;
import de.jansoh.rsistrategy.service.order.OrderUpdateEventListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The PositionService class provides functionality for managing and
 * operating on market positions, including creating positions with
 * associated Take Profit (TP) and Stop Loss (SL) orders, closing
 * positions, and handling updates from order events.
 * <p>
 * This service relies on various dependencies such as API services,
 * repositories for data persistence, and utility registries for managing
 * open positions and handling order event updates.
 */
@Service
@RequiredArgsConstructor
public class PositionService implements OrderUpdateEventListener {

    private static final Logger log = LoggerFactory.getLogger(PositionService.class);

    /**
     * A service interface instance used to interact with the Binance API.
     * This object provides methods for communicating with the Binance
     * cryptocurrency trading platform, including operations such as retrieving
     * market data, managing accounts, and executing trades.
     * <p>
     * This variable is declared as {@code final}, indicating that it is immutable
     * once initialized. It is typically used to ensure consistent access to the
     * Binance API throughout the application.
     */
    private final BinanceApiService binanceApiService;

    /**
     * A repository responsible for handling operations related to orders.
     * Provides an interface for performing CRUD operations and querying order data.
     * This instance is immutable and intended to be used as a dependency within the class.
     */
    private final OrderRepository orderRepository;

    /**
     * Repository interface for accessing and managing position-related data.
     * This component provides methods to perform CRUD operations and queries
     * specifically related to positions in the system.
     */
    private final PositionRepository positionRepository;

    /**
     * An instance of the MessageService class responsible for handling operations
     * and business logic related to message processing within the application.
     * It is declared as a final field to ensure the reference cannot be reassigned
     * after initialization, ensuring thread safety and immutability.
     */
    private final MessageService messageService;

    /**
     * Represents a registry that maintains records of open positions.
     * This object is used to track the state and details of positions
     * that are currently active or unresolved in the system.
     * <p>
     * The OpenPositionRegistry provides mechanisms for adding,
     * removing, and querying open positions, facilitating efficient
     * management and monitoring of these entities.
     */
    private final OpenPositionRegistry openPositionRegistry;

    /**
     * A factory responsible for creating instances of BinanceOrderEventProvider.
     * This factory is used to manage the lifecycle and provision of event
     * provider instances specific to handling order events in the Binance
     * platform. It provides a centralized mechanism to instantiate and
     * configure these providers consistently across the application.
     */
    private final BinanceOrderEventProviderFactory orderEventProviderFactory;

    /**
     * A service responsible for handling precision-related operations.
     * This service provides functionality to perform calculations or
     * manipulations with a defined level of precision applied to data.
     */
    private final PrecisionService precisionService;

    /**
     * A provider responsible for managing and supplying events related to Binance orders.
     * This object encapsulates the logic for interfacing with Binance's order events,
     * ensuring that relevant event data is available for processing and handling.
     */
    private BinanceOrderEventProvider eventProvider;

    /**
     * Initializes the event provider for processing order update events.
     * This method sets up the event provider by creating an instance using the
     * order event provider factory, adding this class as an event listener for
     * order update events, and starting the event provider.
     */
    public void init() {

        eventProvider = orderEventProviderFactory.create();
        eventProvider.addOrderUpdateEventListener(this);
        eventProvider.start();
    }

    /**
     * Creates a trading position with associated Take Profit (TP) and Stop Loss (SL) orders.
     * This method attempts to place a market order for the given position and subsequently creates
     * conditional TP and SL algo orders. Additionally, it can close any existing opposite positions
     * if the `closeOpposites` parameter is set to `true`.
     *
     * @param position       The position object containing the details of the trade, such as symbol, quantity,
     *                       side (LONG/SHORT), timeframe, and TP/SL prices.
     * @param closeOpposites A boolean flag indicating whether or not to close opposite positions
     *                       before creating the new position.
     * @return A boolean value indicating whether the position and associated orders
     * were successfully created. Returns `false` if any order placement fails.
     */
    public boolean createPositionWithTpSl(Position position, boolean closeOpposites) {

        int tries = 10;
        while (tries-- > 0 && !eventProvider.isAvailable()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (0 == tries) {
            log.error("No order event provider running. Trying to restart, but no position will be created this time.");
            eventProvider.start();

            return false;
        }

        // prevent the order event provider from restarting, while creating new positions.
        eventProvider.setPreventRestart(true);

        String side = PositionSide.LONG == position.getSide() ? "BUY" : "SELL";
        String closeSide = PositionSide.LONG == position.getSide() ? "SELL" : "BUY";
        String symbol = position.getSymbol();
        Precision precision = precisionService.getPrecision(symbol);

        AssetTradeWindow atw = AssetTradeWindow.builder()
                .symbol(position.getSymbol())
                .timeframe(position.getTimeframe())
                .build();

        if (closeOpposites && openPositionRegistry.hasPositions(atw)) {

            List<Position> positions = openPositionRegistry.getPositions(atw);

            positions.forEach(p -> {

                // close opposite positions
                if (!p.getSide().equals(position.getSide())) {

                    log.info("----- POSITION_SERVICE ----- closing opposite {} position {} for symbol {} and amount {} on timeframe {}.", p.getSide(), p.getOrderId(), position.getSymbol(), precision.formatQuantity(p.getQuantity()), position.getTimeframe());
                    closeMarketPosition(p);
                }
            });
        }

        // 1. Place Market Order (Entry)
        BinanceOrderRequest entryRequest = BinanceOrderRequest.builder()
                .symbol(position.getSymbol())
                .side(side)
                .type("MARKET")
                .quantity(String.format(precision.formatQuantity(position.getQuantity())))
                .build();

        Map<String, Object> entryResponse;

        try {
            entryResponse = binanceApiService.placeOrder(entryRequest);

            String orderId = entryResponse.get("orderId").toString();
            BigDecimal quantityResponse = new BigDecimal(entryResponse.get("origQty").toString());

            position.setOrderId(orderId);
            position.setQuantity(quantityResponse);

            openPositionRegistry.update(position);

            log.info("----- POSITION_SERVICE ----- New {} {} entry market order at timeframe {} with quantity {} created. The order ID is {}", symbol, side, atw.getTimeframe(), precision.formatQuantity(position.getQuantity()), orderId);

        } catch (BinanceApiServiceOrderException e) {
            log.error("----- POSITION_SERVICE ----- failed placing {} {} entry market order at timeframe {} with quantity {}.", symbol, side, atw.getTimeframe(), precision.formatQuantity(position.getQuantity()), e);
            String msg = String.format("Placing a %s %s entry market order at timeframe %s has failed.\nReason: %s", symbol, side, atw.getTimeframe(), e.getMessage());
            messageService.broadcast(msg);

            return false;
        }

        if (!entryResponse.containsKey("orderId")) {
            log.error("----- POSITION_SERVICE ----- order for {} {} at timeframe {} with quantity: {} has no order ID. Although placing the order succeeded, the order ID is missing.", symbol, side, atw.getTimeframe(), precision.formatQuantity(position.getQuantity()));

            return false;
        }

        String clientOrderId = "algo_tp_" + position.getOrderId() + "_" + position.getTimeframe().getShortcut();

        // 2. Place Take Profit Order
        // Binance Futures: /fapi/v1/algoOrder requires algoType e.g., TAKE_PROFIT
        BinanceAlgoOrderRequest tpRequest = BinanceAlgoOrderRequest.builder()
                .algoType("CONDITIONAL")
                .symbol(symbol)
                .side(closeSide)
                .type("TAKE_PROFIT_MARKET")
                .triggerPrice(precision.formatPrice(position.getTpAlgoPrice()))
                .workingType("MARK_PRICE")
                .priceProtect("TRUE")
                .quantity(precision.formatQuantity(position.getQuantity()))
                .clientAlgoId(clientOrderId)
                .build();

        Map<String, Object> tpResponse;
        try {
            tpResponse = binanceApiService.placeAlgoOrder(tpRequest);

            String symbolResponse = tpResponse.get("symbol").toString();
            String algoIdResponse = tpResponse.get("algoId").toString();
            BigDecimal qtyResponse = new BigDecimal(tpResponse.get("quantity").toString());
            BigDecimal priceResponse = new BigDecimal(tpResponse.get("triggerPrice").toString());

            AlgoOrder tpAlgoOrder = AlgoOrder.builder()
                    .orderId(position.getOrderId())
                    .clientOrderId(clientOrderId)
                    .symbol(symbolResponse)
                    .quantity(qtyResponse)
                    .timeframe(position.getTimeframe())
                    .algoId(algoIdResponse)
                    .type(AlgoOrderType.TP)
                    .triggerPrice(priceResponse)
                    .build();

            openPositionRegistry.update(tpAlgoOrder);

            log.info("----- POSITION_SERVICE ----- New TP algo order for position {} at price: {} created.", position.getOrderId(), precision.formatPrice(position.getTpAlgoPrice()));

        } catch (BinanceApiServiceOrderException e) {

            log.error("----- POSITION_SERVICE ----- failed to create TP algo order for position {}. The position will be closed immediately.", position.getOrderId(), e);

            try {
                closeMarketPosition(position);
                String msg = String.format("Placing a %s %s TP algo order at timeframe %s has failed. The position was closed.\nReason: %s", symbol, side, atw.getTimeframe(), e.getMessage());
                messageService.broadcast(msg);
            } catch (BinanceApiServiceOrderException ex) {
                log.error("----- POSITION_SERVICE ----- position {} could not be closed.", position.getOrderId(), ex);
                String msg = String.format("/!\\ Closing %s %s position at timeframe %s due to an error trying to place a TP algo order at price %.2f has failed.\nTHE POSITION SHOULD BE CLOSED MANUALLY!.\nReason: %s", symbol, side, atw.getTimeframe(), position.getTpAlgoPrice(), e.getMessage());
                messageService.broadcast(msg);
            }
            return false;
        }

        if (!tpResponse.containsKey("algoId")) {
            // can this even happen?
            log.error("----- POSITION_SERVICE ----- TP algo order for position {} has no algo ID, although placing the order succeeded.", position.getOrderId());

            return false;
        }

        clientOrderId = "algo_sl_" + position.getOrderId() + "_" + position.getTimeframe().getShortcut();

        // 3. Place Stop Loss Order
        // Binance Futures: /fapi/v1/algoOrder requires algoType e.g., STOP_LOSS
        BinanceAlgoOrderRequest slRequest = BinanceAlgoOrderRequest.builder()
                .algoType("CONDITIONAL")
                .symbol(symbol)
                .side(closeSide)
                .type("STOP_MARKET")
                .triggerPrice(precision.formatPrice(position.getSlAlgoPrice()))
                .workingType("MARK_PRICE")
                .priceProtect("TRUE")
                .quantity(precision.formatQuantity(position.getQuantity()))
                .clientAlgoId(clientOrderId)
                .build();

        Map<String, Object> slResponse;
        try {
            slResponse = binanceApiService.placeAlgoOrder(slRequest);

            String symbolResponse = slResponse.get("symbol").toString();
            String algoIdResponse = slResponse.get("algoId").toString();
            BigDecimal qtyResponse = new BigDecimal(slResponse.get("quantity").toString());
            BigDecimal priceResponse = new BigDecimal(slResponse.get("triggerPrice").toString());

            AlgoOrder slAlgoOrder = AlgoOrder.builder()
                    .orderId(position.getOrderId())
                    .clientOrderId(clientOrderId)
                    .symbol(symbolResponse)
                    .quantity(qtyResponse)
                    .timeframe(position.getTimeframe())
                    .algoId(algoIdResponse)
                    .type(AlgoOrderType.SL)
                    .triggerPrice(priceResponse)
                    .build();

            openPositionRegistry.update(slAlgoOrder);

            log.info("----- POSITION_SERVICE ----- New SL algo order for position {} at price: {} created.", position.getOrderId(), precision.formatPrice(position.getSlAlgoPrice()));

        } catch (BinanceApiServiceOrderException e) {

            log.error("----- POSITION_SERVICE ----- failed to create SL algo order for position {}. The position will be closed immediately.", position.getOrderId(), e);

            try {
                closeMarketPosition(position);
                String msg = String.format("Placing a %s %s SL algo order at timeframe %s has failed. The position was closed.\nReason: %s", symbol, side, atw.getTimeframe(), e.getMessage());
                messageService.broadcast(msg);
            } catch (BinanceApiServiceOrderException ex) {
                log.error("----- POSITION_SERVICE ----- position {} could not be closed.", position.getOrderId(), ex);
                String msg = String.format("/!\\ Closing %s %s position at timeframe %s due to an error trying to place a SL algo order at price %.2f has failed.\nTHE POSITION SHOULD BE CLOSED MANUALLY!.\nReason: %s", symbol, side, atw.getTimeframe(), position.getSlAlgoPrice(), e.getMessage());
                messageService.broadcast(msg);
            }

            return false;
        }

        if (!slResponse.containsKey("algoId")) {
            // can this even happen?
            log.error("----- POSITION_SERVICE ----- SL algo order for position {} has no algo ID, although placing the order succeeded.", position.getOrderId());

            return false;
        }

        return true;
    }

    /**
     * Closes a market position by creating and executing a market order.
     * If the position is already marked as closed, no new order will be created.
     * Prevents restart of the event system while the position is being closed.
     *
     * @param position The market position to be closed, which contains details such as
     *                 the position's symbol, side (LONG or SHORT), quantity, and order ID.
     */
    public void closeMarketPosition(Position position) {

        if (position.isMarkClosed()) {
            log.warn("----- POSITION_SERVICE ----- position {} was already marked closed. No new order will be created.", position.getOrderId());
            messageService.broadcast(String.format("A %s %s position already marked closed was tried to close again. It could be that order update service is not working.", position.getSymbol(), position.getSide()));
            return;
        }

        eventProvider.setPreventRestart(true);

        Precision p = precisionService.getPrecision(position.getSymbol());

        String clientOrderId = "close_" + position.getOrderId();
        BinanceOrderRequest closeRequest = BinanceOrderRequest.builder()
                .symbol(position.getSymbol())
                .newClientOrderId(clientOrderId)
                .side(PositionSide.LONG.equals(position.getSide()) ? "SELL" : "BUY")
                .type("MARKET")
                .quantity(p.formatQuantity(position.getQuantity()))
                .build();

        binanceApiService.placeOrder(closeRequest);

        position.setMarkClosed(true);

        log.info("----- POSITION_SERVICE ----- position {} was force closed.", position.getOrderId());
    }

    /**
     * Handles updates to an order by processing the provided event.
     *
     * @param event the event containing details about the order update
     */
    @Override
    public void onOrderUpdate(OrderUpdateEvent event) {
        updatePositionFromOrderUpdate(event);
    }

    /**
     * Updates the position data based on the order update event. Processes the order status
     * and either updates an existing open position, persists a closed position, or cancels
     * related take-profit (TP) or stop-loss (SL) algo orders if necessary.
     *
     * @param event the {@link OrderUpdateEvent} containing an updated order and relevant details.
     *              It includes the order's current status, which determines the actions taken on
     *              the associated position.
     */
    public void updatePositionFromOrderUpdate(OrderUpdateEvent event) {

        Order order = event.getOrder();

        if (!order.getOrderStatus().isOneOf(OrderStatus.NEW, OrderStatus.FILLED, OrderStatus.PARTIALLY_FILLED)) {
            log.warn("----- POSITION_SERVICE ----- unsupported order status: {}", order.getOrderStatus());
        }

        orderRepository.save(order);

        Optional<Position> positionOpt = openPositionRegistry.update(order);

        // persist closed positions
        if (positionOpt.isPresent()) {
            Position position = positionOpt.get();
            Precision p = precisionService.getPrecision(position.getSymbol());

            if (position.isClosed()) {

                positionRepository.save(position);

                if (position.isTpOrderFilled()) {
                    log.info("----- POSITION_SERVICE ----- TP order for position {} filled.", position.getOrderId());
                }

                if (position.isSlOrderFilled()) {
                    log.info("----- POSITION_SERVICE ----- SL order for position {} filled.", position.getOrderId());
                }

                String sideIcon = (position.getRealizedProfit().compareTo(BigDecimal.ZERO) < 0) ? "🔴" : "🟢";
                String msg = String.format("""
                                [%s] %s %s Position was closed!
                                Timeframe: %s
                                Size: %s
                                Open Date: %s
                                Close Date: %s
                                Open Price: %s
                                Close Price: %s
                                Profit: %.2f""",
                        sideIcon,
                        position.getSymbol(),
                        position.getSide(),
                        position.getTimeframe(),
                        p.formatPrice(position.getQuantity().multiply(position.getAverageOpenPrice())),
                        position.getOpenTime(),
                        position.getClosedTime(),
                        p.formatPrice(position.getAverageOpenPrice()),
                        p.formatPrice(position.getAverageClosedPrice()),
                        position.getRealizedProfit());


                if (position.hasTpAlgoOrder() && !position.isTpOrderFilled()) {
                    log.info("----- POSITION_SERVICE ----- canceling TP algo order for position {}.", position.getOrderId());
                    binanceApiService.cancelAlgoOrder(position.getTpAlgoId());
                    log.info("----- POSITION_SERVICE ----- TP algo order for position {} was successfully cancelled.", position.getOrderId());
                } else {
                    log.info("----- POSITION_SERVICE ----- canceling TP algo order for position {} skipped. No open order exists.", position.getOrderId());
                }
                if (position.hasSlAlgoOrder() && !position.isSlOrderFilled()) {
                    log.info("----- POSITION_SERVICE ----- canceling SL algo order for position {}.", position.getOrderId());
                    binanceApiService.cancelAlgoOrder(position.getSlAlgoId());
                    log.info("----- POSITION_SERVICE ----- SL algo order for position {} was successfully cancelled.", position.getOrderId());
                } else {
                    log.info("----- POSITION_SERVICE ----- canceling SL algo order for position {} skipped. No open order exists.", position.getOrderId());
                }

                messageService.broadcast(msg);
                log.info(msg);
            } else {

                if (order.getOrderStatus().equals(OrderStatus.FILLED) && order.getOrderId().equals(position.getOrderId())) {
                    String msg = String.format("""
                                    New %s %s Position entered!
                                    Timeframe: %s
                                    Size: %s USDT
                                    Open Date: %s
                                    Open Price: %s""",
                            position.getSymbol(),
                            position.getSide(),
                            position.getTimeframe(),
                            p.formatPrice(position.getQuantity().multiply(position.getAverageOpenPrice())),
                            position.getOpenTime(),
                            p.formatPrice(position.getAverageOpenPrice()));

                    messageService.broadcast(msg);
                }
            }
        }

        if (OrderStatus.FILLED == order.getOrderStatus()) {
            eventProvider.setPreventRestart(false);
        }
    }
}

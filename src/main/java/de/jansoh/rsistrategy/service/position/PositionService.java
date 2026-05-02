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

@Service
@RequiredArgsConstructor
public class PositionService implements OrderUpdateEventListener {

    private static final Logger log = LoggerFactory.getLogger(PositionService.class);

    private final BinanceApiService binanceApiService;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final MessageService messageService;
    private final OpenPositionRegistry openPositionRegistry;
    private final BinanceOrderEventProviderFactory orderEventProviderFactory;
    private final PrecisionService precisionService;

    private BinanceOrderEventProvider eventProvider;

    public void init() {

        eventProvider = orderEventProviderFactory.create();
        eventProvider.addOrderUpdateEventListener(this);
        eventProvider.start();
    }

    /**
     * Create a new position with Market entry and TP/SL orders.
     * If market order fails, TP/SL are not created.
     * If any TP/SL order fails, the market order is closed immediately.
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

    @Override
    public void onOrderUpdate(OrderUpdateEvent event) {
        updatePositionFromOrderUpdate(event);
    }

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

package de.jansoh.rsistrategy.service.position;

import de.jansoh.rsistrategy.model.*;
import de.jansoh.rsistrategy.repository.OrderRepository;
import de.jansoh.rsistrategy.repository.PositionRepository;
import de.jansoh.rsistrategy.service.BinanceApiService;
import de.jansoh.rsistrategy.service.BinanceApiServiceOrderException;
import de.jansoh.rsistrategy.service.TelegramMessagingService;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProvider;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProviderFactory;
import de.jansoh.rsistrategy.service.order.OrderUpdateEvent;
import de.jansoh.rsistrategy.service.order.OrderUpdateEventListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final TelegramMessagingService telegramMessagingService;
    private final OpenPositionRegistry openPositionRegistry;
    private final BinanceOrderEventProviderFactory orderEventProviderFactory;


    public void init() {

        BinanceOrderEventProvider eventProvider = orderEventProviderFactory.create();
        eventProvider.addOrderUpdateEventListener(this);

        Thread eventProviderThread = new Thread(eventProvider);
        eventProviderThread.start();
    }

    /**
     * Create a new position with Market entry and TP/SL orders.
     * If market order fails, TP/SL are not created.
     * If any TP/SL order fails, the market order is closed immediately.
     */
    public boolean createPositionWithTpSl(Position position, boolean closeOpposites) {
        String side = PositionSide.LONG == position.getSide() ? "BUY" : "SELL";
        String closeSide = PositionSide.LONG == position.getSide() ? "SELL" : "BUY";
        String symbol = position.getSymbol();

        AssetTradeWindow atw = AssetTradeWindow.builder()
                .symbol(position.getSymbol())
                .timeframe(position.getTimeframe())
                .build();

        if (closeOpposites && openPositionRegistry.hasPositions(atw)) {

            List<Position> positions = openPositionRegistry.getPositions(atw);

            positions.forEach(p -> {

                // close opposite positions
                if (!p.getSide().equals(position.getSide())) {

                    log.info("----- POSITION_SERVICE ----- closing opposite {} position {} for symbol {} and amount {} on timeframe {}.", p.getSide(), p.getOrderId(), position.getSymbol(), p.getQuantity().setScale(4, RoundingMode.HALF_UP), position.getTimeframe());
                    closeMarketPosition(p);
                }
            });
        }

        // 1. Place Market Order (Entry)
        BinanceOrderRequest entryRequest = BinanceOrderRequest.builder()
                .symbol(position.getSymbol())
                .side(side)
                .type("MARKET")
                .quantity(String.format("%.3f", position.getQuantity()))
                .build();

        log.info("----- POSITION_SERVICE ----- placing Entry Market Order for {} side: {}, quantity: {}", symbol, side, position.getQuantity().setScale(4, RoundingMode.HALF_UP));
        Map<String, Object> entryResponse;

        try {
            entryResponse = binanceApiService.placeOrder(entryRequest);

            String orderId = entryResponse.get("orderId").toString();
            BigDecimal quantityResponse = new BigDecimal(entryResponse.get("origQty").toString());

            position.setOrderId(orderId);
            position.setQuantity(quantityResponse);

            openPositionRegistry.update(position);

        } catch (BinanceApiServiceOrderException e) {
            log.error("----- POSITION_SERVICE ----- failed placing entry market order for {} side: {}, quantity: {}", symbol, side, position.getQuantity().setScale(4, RoundingMode.HALF_UP), e);
            String msg = String.format("----- POSITION_SERVICE ----- failed to place Entry Market Order for %s. Reason: %s", symbol, e.getMessage());
            telegramMessagingService.broadcast(msg);
            return false;
        }

        if (!entryResponse.containsKey("orderId")) {
            log.error("----- POSITION_SERVICE ----- order for {} side: {}, quantity: {} has no order ID. Although placing the order succeeded, the order ID is missing.", symbol, side, position.getQuantity().setScale(4, RoundingMode.HALF_UP));
            return false;
        }

        String clientOrderId = "algo_tp_oid_" + position.getOrderId() + "_tf_" + position.getTimeframe().getShortcut();

        // 2. Place Take Profit Order
        // Binance Futures: /fapi/v1/algoOrder requires algoType e.g., TAKE_PROFIT
        BinanceAlgoOrderRequest tpRequest = BinanceAlgoOrderRequest.builder()
                .algoType("CONDITIONAL")
                .symbol(symbol)
                .side(closeSide)
                .type("TAKE_PROFIT_MARKET")
                .triggerPrice(String.format("%.2f", position.getTpAlgoPrice()))
                .workingType("MARK_PRICE")
                .priceProtect("TRUE")
                .quantity(String.format("%.3f", position.getQuantity()))
                .clientAlgoId(clientOrderId)
                .build();

        log.info("----- POSITION_SERVICE ----- placing TP algo order for position {} at price: {}", position.getOrderId(), position.getTpAlgoPrice().setScale(4, RoundingMode.HALF_UP));
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

        } catch (BinanceApiServiceOrderException e) {
            log.error(e.getMessage(), e);

            try {
                log.error("----- POSITION_SERVICE ----- failed to create TP for position {}, it will be closed immediately.", position.getOrderId());
                closeMarketPosition(position);
                String msg = String.format("Failed to place Algo TP Order for %s. Reason: %s. The position has been closed.", symbol, e.getMessage());
                telegramMessagingService.broadcast(msg);
            } catch (BinanceApiServiceOrderException ex) {
                log.error("----- POSITION_SERVICE ----- position {} could not be closed.", position.getOrderId(), ex);
                String msg = String.format("( ! ) Failed to place Algo TP Order for %s. Reason: %s. The position COULD NOT BE CLOSED.", symbol, e.getMessage());
                telegramMessagingService.broadcast(msg);
            }
            return false;
        }

        if (!tpResponse.containsKey("algoId")) {
            // can this even happen?
            log.error("----- POSITION_SERVICE ----- TP algo order for position {} has no algo ID, although placing the order succeeded.", position.getOrderId());
            return false;
        }

        clientOrderId = "algo_sl_oid_" + position.getOrderId() + "_tf_" + position.getTimeframe().getShortcut();

        // 3. Place Stop Loss Order
        // Binance Futures: /fapi/v1/algoOrder requires algoType e.g., STOP_LOSS
        BinanceAlgoOrderRequest slRequest = BinanceAlgoOrderRequest.builder()
                .algoType("CONDITIONAL")
                .symbol(symbol)
                .side(closeSide)
                .type("STOP_MARKET")
                .triggerPrice(String.format("%.2f", position.getSlAlgoPrice()))
                .workingType("MARK_PRICE")
                .priceProtect("TRUE")
                .quantity(String.format("%.3f", position.getQuantity()))
                .clientAlgoId(clientOrderId)
                .build();

        log.info("----- POSITION_SERVICE ----- placing SL algo order for position {} at price: {}", position.getOrderId(), position.getSlAlgoPrice().setScale(4, RoundingMode.HALF_UP));
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

        } catch (BinanceApiServiceOrderException e) {
            log.error(e.getMessage(), e);

            try {
                log.error("----- POSITION_SERVICE ----- failed to create SL for position {}, it will be closed immediately.", position.getOrderId());
                closeMarketPosition(position);
                String msg = String.format("Failed to place Algo SL Order for %s. Reason: %s. The position has been closed.", symbol, e.getMessage());
                telegramMessagingService.broadcast(msg);
            } catch (BinanceApiServiceOrderException ex) {
                log.error("----- POSITION_SERVICE ----- position {} could not be closed.", position.getOrderId(), ex);
                String msg = String.format("( ! ) Failed to place Algo SL Order for %s. Reason: %s. The position COULD NOT BE CLOSED.", symbol, e.getMessage());
                telegramMessagingService.broadcast(msg);
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
        String clientOrderId = "close_" + position.getOrderId();
        BinanceOrderRequest closeRequest = BinanceOrderRequest.builder()
                .symbol(position.getSymbol())
                .newClientOrderId(clientOrderId)
                .side(PositionSide.LONG.equals(position.getSide()) ? "SELL" : "BUY")
                .type("MARKET")
                .quantity(String.format("%.4f", position.getQuantity()))
                .build();
        binanceApiService.placeOrder(closeRequest);
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
                                %s Position was closed!
                                Symbol: %s
                                Side: %s
                                Size: %.2f
                                Open Date: %s
                                Close Date: %s
                                Open Price: %.2f
                                Close Price: %.2f
                                Profit: %.2f""",
                        sideIcon,
                        position.getSymbol(),
                        position.getSide(),
                        position.getQuantity().multiply(position.getAverageOpenPrice()),
                        position.getOpenTime(),
                        position.getClosedTime(),
                        position.getAverageOpenPrice(),
                        position.getAverageClosedPrice(),
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

                telegramMessagingService.broadcast(msg);
                log.info(msg);
            } else {

                if (order.getOrderStatus().equals(OrderStatus.FILLED) && order.getOrderId().equals(position.getOrderId())) {
                    String msg = String.format("""
                                    Position entered!
                                    Symbol: %s
                                    Side: %s
                                    Size: %.2f
                                    Open Date: %s
                                    Open Price: %.2f""",
                            position.getSymbol(),
                            position.getSide(),
                            position.getQuantity().multiply(position.getAverageOpenPrice()),
                            position.getOpenTime(),
                            position.getAverageOpenPrice());

                    telegramMessagingService.broadcast(msg);
                }
            }
        }
    }
}

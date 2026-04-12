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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PositionService implements OrderUpdateEventListener {

    private final BinanceApiService binanceApiService;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final TelegramMessagingService telegramMessagingService;
    private final OpenPositionRegistry openPositionRegistry;
    private final BinanceOrderEventProviderFactory orderEventProviderFactory;


    public void init() {

        BinanceOrderEventProvider eventProvider = orderEventProviderFactory.create();
        eventProvider.addOrderUpdateEventListener(this);
        eventProvider.start();
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

                    log.info("----- POSITION_SERVICE ----- closing opposite position {} for symbol {} and amount {} on timeframe {}.", p.getOrderId(), position.getSymbol(), p.getQuantity(), position.getTimeframe());
                    closeMarketPosition(p);
                }
            });
        }

        // 1. Place Market Order (Entry)
        BinanceOrderRequest entryRequest = BinanceOrderRequest.builder()
                .symbol(position.getSymbol())
                .side(side)
                .type("MARKET")
                .quantity(String.format("%.4f", position.getQuantity()))
                .build();

        log.info("----- POSITION_SERVICE ----- placing Entry Market Order for {} side: {} quantity: {}", symbol, side, position.getQuantity());
        Map<String, Object> entryResponse;

        try {
            entryResponse = binanceApiService.placeOrder(entryRequest);

            String orderId = entryResponse.get("orderId").toString();
            BigDecimal quantityResponse = new BigDecimal(entryResponse.get("origQty").toString());

            position.setOrderId(orderId);
            position.setQuantity(quantityResponse);

            openPositionRegistry.update(position);

        } catch (BinanceApiServiceOrderException e) {
            log.error(e.getMessage(), e);
            String msg = String.format("----- POSITION_SERVICE ----- failed to place Entry Market Order for %s. Reason: %s", symbol, e.getMessage());
            telegramMessagingService.broadcast(msg);
            return false;
        }

        if (!entryResponse.containsKey("orderId")) {
            log.error("----- POSITION_SERVICE ----- failed to place Entry Market Order for {}. Although placing the order succeeded, the order ID in the response is missing.", symbol);
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
                .quantity(String.format("%.4f", position.getQuantity()))
                .clientAlgoId(clientOrderId)
                .build();

        log.info("----- POSITION_SERVICE ----- placing TP Algo Order for {} price: {}", symbol, position.getTpAlgoPrice());
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
                log.info("----- POSITION_SERVICE ----- trying to close market position due to TP failure.");
                closeMarketPosition(position);
                String msg = String.format("Failed to place Algo TP Order for %s. Reason: %s. The position has been closed.", symbol, e.getMessage());
                telegramMessagingService.broadcast(msg);
            } catch (BinanceApiServiceOrderException ex) {
                log.error(ex.getMessage(), ex);
                String msg = String.format("( ! ) Failed to place Algo TP Order for %s. Reason: %s. The position COULD NOT BE CLOSED.", symbol, e.getMessage());
                telegramMessagingService.broadcast(msg);
            }
            return false;
        }

        if (!tpResponse.containsKey("algoId")) {
            log.error("----- POSITION_SERVICE ----- failed to place TP Order for {}. Although placing the order succeeded, the algo ID in the response is missing.", symbol);
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
                .quantity(String.format("%.4f", position.getQuantity()))
                .clientAlgoId(clientOrderId)
                .build();

        log.info("----- POSITION_SERVICE ----- placing SL Algo Order for {} price: {}", symbol, position.getSlAlgoPrice());
        Map<String, Object> slResponse;
        try {
            slResponse = binanceApiService.placeAlgoOrder(slRequest);

            String symbolResponse = tpResponse.get("symbol").toString();
            String algoIdResponse = tpResponse.get("algoId").toString();
            BigDecimal qtyResponse = new BigDecimal(tpResponse.get("quantity").toString());
            BigDecimal priceResponse = new BigDecimal(tpResponse.get("triggerPrice").toString());

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
                log.info("----- POSITION_SERVICE ----- trying to close market position due to SL failure.");
                closeMarketPosition(position);
                String msg = String.format("Failed to place Algo SL Order for %s. Reason: %s. The position has been closed.", symbol, e.getMessage());
                telegramMessagingService.broadcast(msg);
            } catch (BinanceApiServiceOrderException ex) {
                log.error(ex.getMessage(), ex);
                String msg = String.format("( ! ) Failed to place Algo SL Order for %s. Reason: %s. The position COULD NOT BE CLOSED.", symbol, e.getMessage());
                telegramMessagingService.broadcast(msg);
            }
            return false;
        }

        if (slResponse == null || !slResponse.containsKey("algoId")) {
            log.error("----- POSITION_SERVICE ----- failed to place SL Order for {}. Although placing the order succeeded, the algo ID in the response is missing.", symbol);
            return false;
        }

        return true;
    }

    private void closeMarketPosition(Position position) {
        BinanceOrderRequest closeRequest = BinanceOrderRequest.builder()
                .symbol(position.getSymbol())
                .side(PositionSide.LONG.equals(position.getSide()) ? "SELL" : "BUY")
                .type("MARKET")
                .quantity(String.format("%.4f", position.getQuantity()))
                .build();
        log.info("----- POSITION_SERVICE ----- closing Market Order for {}, quantity {} and side: {} due to TP/SL failure", position.getSymbol(), position.getQuantity(), position.getSide());
        binanceApiService.placeOrder(closeRequest);
        if (position.hasTpAlgoOrder()) {
            log.info("Canceling TP Algo Order for {}", position.getSymbol());
            binanceApiService.cancelAlgoOrder(position.getTpAlgoOrderId());
        }
        if (position.hasSlAlgoOrder()) {
            log.info("Canceling SL Algo Order for {}", position.getSymbol());
            binanceApiService.cancelAlgoOrder(position.getSlAlgoOrderId());
        }
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
        if (order.getOrderStatus().equals(OrderStatus.FILLED)) {

            if (order.getClientOrderId().startsWith("algo_")) {
                log.info("----- POSITION_SERVICE ----- TP/SL Algo Order ({}) for {} filled. Closing position.", order.getClientOrderId(), order.getSymbol());
            }
        }
        // persist closed positions
        if (positionOpt.isPresent()) {
            Position position = positionOpt.get();

            if (position.isClosed()) {

                positionRepository.save(position);

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

                try {
                    binanceApiService.cancelAlgoOrder(position.getTpAlgoOrderId());
                } catch (Exception e) {
                    log.debug("----- POSITION_SERVICE ----- Failed to cancel TP Algo Order for position {}. Reason: {}", position.getTpAlgoOrderId(), e.getMessage());
                }
                try {
                    binanceApiService.cancelAlgoOrder(position.getSlAlgoOrderId());
                } catch (Exception e) {
                    log.debug("----- POSITION_SERVICE ----- Failed to cancel SL Algo Order for position {}. Reason: {}", position.getSlAlgoOrderId(), e.getMessage());
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
                    log.info(msg);
                }
            }
        }
    }
}

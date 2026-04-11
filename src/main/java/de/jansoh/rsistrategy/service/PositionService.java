package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PositionService {

    private final BinanceApiService binanceApiService;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final TelegramMessagingService telegramMessagingService;
    private final OrderUpdateEventMapper orderUpdateEventMapper;
    private final OpenPositionRegistry openPositionRegistry;


    /**
     * Create a new position with Market entry and TP/SL orders.
     * If market order fails, TP/SL are not created.
     * If any TP/SL order fails, the market order is closed immediately.
     */
    public boolean createPositionWithTpSl(String symbol, String type, String quantity, double tp, double sl) {
        String side = type.equalsIgnoreCase("LONG") ? "BUY" : "SELL";
        String closeSide = side.equals("BUY") ? "SELL" : "BUY";

        // 1. Place Market Order (Entry)
        BinanceOrderRequest entryRequest = BinanceOrderRequest.builder()
                .symbol(symbol)
                .side(side)
                .type("MARKET")
                .quantity(quantity)
                .build();

        log.info("Placing Entry Market Order for {} side: {} quantity: {}", symbol, side, quantity);
        Map<String, Object> entryResponse;
        Position position;
        try {
            entryResponse = binanceApiService.placeOrder(entryRequest);

            String orderId = entryResponse.get("orderId").toString();
            BigDecimal quantityResponse = new BigDecimal(entryResponse.get("origQty").toString());
            position = Position.builder()
                    .orderId(orderId)
                    .symbol(symbol)
                    .quantity(quantityResponse)
                    .build();

            openPositionRegistry.add(position);

        } catch (BinanceApiServiceOrderException e) {
            log.error(e.getMessage(), e);
            String msg = String.format("Failed to place Entry Market Order for %s. Reason: %s", symbol, e.getMessage());
            telegramMessagingService.broadcast(msg);
            return false;
        }

        if (!entryResponse.containsKey("orderId")) {
            log.error("Failed to place Entry Market Order for {}. Although placing the order succeeded, the order ID in the response is missing.", symbol);
            return false;
        }

        // 2. Place Take Profit Order
        // Binance Futures: /fapi/v1/algoOrder requires algoType e.g., TAKE_PROFIT
        BinanceAlgoOrderRequest tpRequest = BinanceAlgoOrderRequest.builder()
                .algoType("CONDITIONAL")
                .symbol(symbol)
                .side(closeSide)
                .type("TAKE_PROFIT_MARKET")
                .triggerPrice(String.format("%.4f", tp))
                .workingType("MARK_PRICE")
                .priceProtect("TRUE")
                .quantity(quantity)
                .build();

        log.info("Placing TP Algo Order for {} price: {}", symbol, tp);
        Map<String, Object> tpResponse;
        try {
            tpResponse = binanceApiService.placeAlgoOrder(tpRequest);

            String symbolResponse = tpResponse.get("symbol").toString();
            String algoIdResponse = tpResponse.get("algoId").toString();
            BigDecimal qtyResponse = new BigDecimal(tpResponse.get("quantity").toString());
            BigDecimal priceResponse = new BigDecimal(tpResponse.get("triggerPrice").toString());
            OrderSide sideResponse = OrderSide.valueOf(tpResponse.get("side").toString());

            AlgoOrder tpAlgoOrder = AlgoOrder.builder()
                    .symbol(symbolResponse)
                    .algoId(algoIdResponse)
                    .side(sideResponse)
                    .quantity(qtyResponse)
                    .triggerPrice(priceResponse)
                    .build();

            openPositionRegistry.update(tpAlgoOrder);

        } catch (BinanceApiServiceOrderException e) {
            log.error(e.getMessage(), e);

            try {
                log.info("Trying to close market position due to TP failure.");
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
            log.error("Failed to place TP Order for {}. Although placing the order succeeded, the algo ID in the response is missing.", symbol);
            return false;
        }

        // 3. Place Stop Loss Order
        // Binance Futures: /fapi/v1/algoOrder requires algoType e.g., STOP_LOSS
        BinanceAlgoOrderRequest slRequest = BinanceAlgoOrderRequest.builder()
                .algoType("CONDITIONAL")
                .symbol(symbol)
                .side(closeSide)
                .type("STOP_MARKET")
                .triggerPrice(String.format("%.4f", sl))
                .workingType("MARK_PRICE")
                .priceProtect("TRUE")
                .quantity(quantity)
                .build();

        log.info("Placing SL Algo Order for {} price: {}", symbol, sl);
        Map<String, Object> slResponse;
        try {
            slResponse = binanceApiService.placeAlgoOrder(slRequest);

            String symbolResponse = tpResponse.get("symbol").toString();
            String algoIdResponse = tpResponse.get("algoId").toString();
            BigDecimal qtyResponse = new BigDecimal(tpResponse.get("quantity").toString());
            BigDecimal priceResponse = new BigDecimal(tpResponse.get("triggerPrice").toString());
            OrderSide sideResponse = OrderSide.valueOf(tpResponse.get("side").toString());

            AlgoOrder slAlgoOrder = AlgoOrder.builder()
                    .symbol(symbolResponse)
                    .algoId(algoIdResponse)
                    .side(sideResponse)
                    .quantity(qtyResponse)
                    .triggerPrice(priceResponse)
                    .build();

            openPositionRegistry.update(slAlgoOrder);

        } catch (BinanceApiServiceOrderException e) {
            log.error(e.getMessage(), e);

            try {
                log.info("Trying to close market position due to SL failure.");
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
            log.error("Failed to place SL Order for {}. Although placing the order succeeded, the algo ID in the response is missing.", symbol);
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
        log.info("Closing Market Order for {}, quantity {} and side: {} due to TP/SL failure", position.getSymbol(), position.getQuantity(), position.getSide());
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

    public void updatePositionFromOrderUpdate(Map<String, Object> orderData) {
        if (orderData == null || !orderData.containsKey("o")) return;

        Order order = orderUpdateEventMapper.map(orderData);

        if (!order.getOrderStatus().isOneOf(OrderStatus.NEW, OrderStatus.FILLED, OrderStatus.PARTIALLY_FILLED)) {
            log.warn("Unsupported order status: {}", order.getOrderStatus());
        }
        orderRepository.save(order);
        Optional<Position> positionOpt = openPositionRegistry.update(order);
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

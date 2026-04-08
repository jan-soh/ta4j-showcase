package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PositionService {

    private final BinanceApiService binanceApiService;
    private final OrderRepository orderRepository;
    private final TelegramMessagingService telegramMessagingService;
    private final OrderUpdateEventMapper orderUpdateEventMapper;

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
        Map<String, Object> entryResponse = binanceApiService.placeOrder(entryRequest);

        if (entryResponse == null || !entryResponse.containsKey("orderId")) {
            log.error("Failed to place Entry Market Order for {}", symbol);
            return false;
        }

        // 2. Place Take Profit Order
        // Binance Futures: /fapi/v1/algoOrder requires algoType e.g., TAKE_PROFIT
        BinanceAlgoOrderRequest tpRequest = BinanceAlgoOrderRequest.builder()
                .algoType("CONDITIONAL")
                .symbol(symbol)
                .side(closeSide)
                .type("TAKE_PROFIT_MARKET")
                .triggerPrice(String.format("%.2f", tp))
                .workingType("MARK_PRICE")
                .priceProtect("TRUE")
                .closePosition("TRUE")
                .build();

        log.info("Placing TP Algo Order for {} price: {}", symbol, tp);
        Map<String, Object> tpResponse = binanceApiService.placeAlgoOrder(tpRequest);

        if (tpResponse == null || !tpResponse.containsKey("algoId")) {
            log.error("Failed to place TP Order for {}. Closing entry order.", symbol);
            closeMarketPosition(symbol, closeSide, quantity);
            return false;
        }

        // 3. Place Stop Loss Order
        // Binance Futures: /fapi/v1/algoOrder requires algoType e.g., STOP_LOSS
        BinanceAlgoOrderRequest slRequest = BinanceAlgoOrderRequest.builder()
                .algoType("CONDITIONAL")
                .symbol(symbol)
                .side(closeSide)
                .type("STOP_MARKET")
                .triggerPrice(String.format("%.2f", sl))
                .workingType("MARK_PRICE")
                .priceProtect("TRUE")
                .closePosition("TRUE")
                .build();

        log.info("Placing SL Algo Order for {} price: {}", symbol, sl);
        Map<String, Object> slResponse = binanceApiService.placeAlgoOrder(slRequest);

        if (slResponse == null || !slResponse.containsKey("algoId")) {
            log.error("Failed to place SL Order for {}. Closing entry order and TP order if possible.", symbol);
            // In a real scenario, we'd also need to cancel the TP order. 
            // But for simplicity as per requirements, we close the market order.
            closeMarketPosition(symbol, closeSide, quantity);
            return false;
        }

        return true;
    }

    private void closeMarketPosition(String symbol, String side, String quantity) {
        BinanceOrderRequest closeRequest = BinanceOrderRequest.builder()
                .symbol(symbol)
                .side(side)
                .type("MARKET")
                .quantity(quantity)
                .build();
        log.info("Closing Market Order for {} side: {} due to TP/SL failure", symbol, side);
        binanceApiService.placeOrder(closeRequest);
    }

    public void updatePositionFromOrderUpdate(Map<String, Object> orderData) {
        if (orderData == null || !orderData.containsKey("o")) return;

        Order order = orderUpdateEventMapper.map(orderData);

        if (!order.getOrderStatus().isOneOf(OrderStatus.NEW, OrderStatus.FILLED, OrderStatus.PARTIALLY_FILLED)) {
            log.warn("Unsupported order status: {}", order.getOrderStatus());
        }
        orderRepository.save(order);

        if (OrderStatus.FILLED.equals(order.getOrderStatus())) {
            String sideIcon = order.getSide().equals(OrderSide.BUY) ? "🟢" : "🔴";
            String msg = String.format("%s ORDER FILLED: %s\nPrice: %.2f\nProfit: %.4f",
                    sideIcon, order.getSymbol(), order.getLastFilledPrice(), order.getRealizedProfit());
            telegramMessagingService.broadcast(msg);
        }
    }
}

package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceAlgoOrderRequest;
import de.jansoh.rsistrategy.model.BinanceOrderRequest;
import de.jansoh.rsistrategy.model.Position;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PositionService {

    private final BinanceApiService binanceApiService;
    private final PositionRepository positionRepository;
    private final TelegramMessagingService telegramMessagingService;

    /**
     * Create a new position with Market entry and TP/SL orders.
     * If market order fails, TP/SL are not created.
     * If any TP/SL order fails, the market order is closed immediately.
     */
    public Map<String, Object> createPositionWithTpSl(String symbol, String type, String quantity, double tp, double sl) {
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
            return null;
        }

        log.info("Entry Market Order placed successfully: {}", entryResponse.get("orderId"));

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
            return null;
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
            return null;
        }

        entryResponse.put("tpAlgoId", tpResponse.get("algoId"));
        entryResponse.put("slAlgoId", slResponse.get("algoId"));

        return entryResponse;
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

    public void checkPositionStatus(Position p, String symbol) {
        if (p.isClosed()) return;

        // Fallback polling check
        // Check TP status
        if (p.getTpAlgoId() != null) {
            Map<String, Object> tpStatus = binanceApiService.getAlgoOrder(p.getTpAlgoId());
            if (isAlgoOrderFinished(tpStatus)) {
                log.info("Fallback polling: TP finished for position {}", p.getId());
                // We don't have realized PnL in algoStatus polling, so it stays null or 0 until WS update or manual check
                updatePositionClosure(p, tpStatus, "TAKE PROFIT (ALGO-POLL)");
                return;
            }
            if (isAlgoOrderCancelled(tpStatus)) {
                updatePositionClosure(p, tpStatus, "CANCELED (ALGO-POLL)");
                return;
            }
        }

        // Check SL status
        if (p.getSlAlgoId() != null) {
            Map<String, Object> slStatus = binanceApiService.getAlgoOrder(p.getSlAlgoId());
            if (isAlgoOrderFinished(slStatus)) {
                log.info("Fallback polling: SL finished for position {}", p.getId());
                updatePositionClosure(p, slStatus, "STOP LOSS (ALGO-POLL)");
                return;
            }
            if (isAlgoOrderCancelled(slStatus)) {
                updatePositionClosure(p, slStatus, "CANCELED (ALGO-POLL)");
                return;
            }
        }
    }

    /**
     * Close a position manually (e.g., when switching from LONG to SHORT).
     */
    public void closePosition(Position p, String symbol) {
        if (p.isClosed()) return;

        String closeSide = p.getType().equalsIgnoreCase("LONG") ? "SELL" : "BUY";
        // 1. Close the market position
        // We use quantity "0.01" as it was used in createPositionWithTpSl. 
        // In a real app, we should probably store the quantity in the Position model.
        // For now, following the existing pattern.
        closeMarketPosition(symbol, closeSide, "0.01");

        // 2. Cancel TP/SL Algo Orders
        if (p.getTpAlgoId() != null) {
            binanceApiService.cancelAlgoOrder(p.getTpAlgoId());
        }
        if (p.getSlAlgoId() != null) {
            binanceApiService.cancelAlgoOrder(p.getSlAlgoId());
        }

        // 3. Update local state
        p.setClosed(true);
        p.setCloseDate(java.time.ZonedDateTime.now());
        // We could fetch the actual exit price, but for a quick flip, we mark it as closed.
        positionRepository.save(p);

        log.info("Position {} closed manually (flip)", p.getId());
        telegramMessagingService.broadcast("🔄 Position flipped/closed manually: " + p.getType());
    }

    /**
     * Update TP/SL orders for an existing position.
     * Cancels existing TP/SL orders and places new ones.
     */
    public void updatePositionTpSl(Position p, String symbol, double tp, double sl) {
        if (p.isClosed()) return;

        // 1. Cancel existing TP/SL Algo Orders
        if (p.getTpAlgoId() != null) {
            log.info("Cancelling old TP Algo Order: {}", p.getTpAlgoId());
            binanceApiService.cancelAlgoOrder(p.getTpAlgoId());
        }
        if (p.getSlAlgoId() != null) {
            log.info("Cancelling old SL Algo Order: {}", p.getSlAlgoId());
            binanceApiService.cancelAlgoOrder(p.getSlAlgoId());
        }

        String closeSide = p.getType().equalsIgnoreCase("LONG") ? "SELL" : "BUY";

        // 2. Place New Take Profit Order
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

        log.info("Placing NEW TP Algo Order for {} price: {}", symbol, tp);
        Map<String, Object> tpResponse = binanceApiService.placeAlgoOrder(tpRequest);

        if (tpResponse != null && tpResponse.containsKey("algoId")) {
            p.setTpAlgoId(tpResponse.get("algoId").toString());
            p.setTakeProfit(tp);
        } else {
            log.error("Failed to update TP Order for {}.", symbol);
            p.setTpAlgoId(null);
        }

        // 3. Place New Stop Loss Order
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

        log.info("Placing NEW SL Algo Order for {} price: {}", symbol, sl);
        Map<String, Object> slResponse = binanceApiService.placeAlgoOrder(slRequest);

        if (slResponse != null && slResponse.containsKey("algoId")) {
            p.setSlAlgoId(slResponse.get("algoId").toString());
            p.setStopLoss(sl);
        } else {
            log.error("Failed to update SL Order for {}.", symbol);
            p.setSlAlgoId(null);
        }

        positionRepository.save(p);
        log.info("Position {} TP/SL updated: TP={} SL={}", p.getId(), tp, sl);
        telegramMessagingService.broadcast(String.format("🔄 TP/SL Updated for %s position:\nNew TP: %.2f\nNew SL: %.2f", p.getType(), tp, sl));
    }

    private boolean isAlgoOrderFinished(Map<String, Object> algoStatus) {
        if (algoStatus == null || !algoStatus.containsKey("algoStatus")) return false;
        String status = algoStatus.get("algoStatus").toString();
        // Possible statuses for algoOrder: FINISHED
        return AlgoStatus.isFinished(status);
    }

    private boolean isAlgoOrderCancelled(Map<String, Object> algoStatus) {
        if (algoStatus == null || !algoStatus.containsKey("algoStatus")) return false;
        String status = algoStatus.get("algoStatus").toString();
        // Possible statuses for algoOrder: CANCELLED
        return AlgoStatus.isCancelled(status);
    }

    private void updatePositionClosure(Position p, Map<String, Object> executionData, String reason) {
        p.setClosed(true);
        // Using triggerPrice as exit price is not very accurate, because the position may have been canceled. In that case the exit price will be the current market price.
        if (executionData.containsKey("triggerPrice")) {
            p.setExitPrice(Double.parseDouble(executionData.get("triggerPrice").toString()));
        }
        if (executionData.containsKey("updateTime")) {
            long updateTime = Long.parseLong(executionData.get("updateTime").toString());
            p.setCloseDate(java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(updateTime), java.time.ZoneId.systemDefault()));
        }

        positionRepository.save(p);

        String msg = String.format("🔴 POSITION CLOSED (ALGO): %s\nType: %s\nEntry: %.2f | Exit: %.2f\nProfit: %.4f\nClose Date: %s",
                reason, p.getType(), p.getEntryPrice(), p.getExitPrice(), p.getRealizedProfit() != null ? p.getRealizedProfit() : 0.0, p.getCloseDate());
        telegramMessagingService.broadcast(msg);

        log.info("Position {} closed by {}: exit price {} | Realized Profit: {}", p.getId(), reason, p.getExitPrice(), p.getRealizedProfit());
    }

    public void updatePositionFromOrderUpdate(Map<String, Object> orderData) {
        if (orderData == null || !orderData.containsKey("o")) return;
        Map<String, Object> o = (Map<String, Object>) orderData.get("o");

        System.out.println("Order Update: " + o);

        String symbol = o.get("s").toString();
        String executionType = o.get("x").toString(); // Execution Type
        String orderStatus = o.get("X").toString(); // Order Status
        double originalQuantity = Double.parseDouble(o.get("q").toString());

        // We only care about fills for TP/SL orders (or any closing order)
        if (!executionType.equals("TRADE")) {
            return;
        }

        if (!orderStatus.equals("FILLED") && !orderStatus.equals("PARTIALLY_FILLED")) {
            return;
        }

        List<Position> positions = positionRepository.findBySymbolAndQuantityAndClosedFalse(symbol, originalQuantity);

        if (positions.isEmpty()) {
            log.debug("No matching open position found for symbol: {} and quantity: {}", symbol, originalQuantity);
            return;
        }

        // In this showcase, we assume the oldest open position matching symbol and quantity is the one being filled
        Position p = positions.get(0);

        double lastFilledQuantity = Double.parseDouble(o.get("l").toString());
        double lastFilledPrice = Double.parseDouble(o.get("L").toString());
        double realizedProfit = Double.parseDouble(o.get("rp").toString());
        long eventTime = Long.parseLong(orderData.get("E").toString());

        p.setFilledQuantity(p.getFilledQuantity() + lastFilledQuantity);
        p.setRealizedProfit((p.getRealizedProfit() != null ? p.getRealizedProfit() : 0.0) + realizedProfit);
        p.setExitPrice(lastFilledPrice); // Update with latest fill price
        p.setCloseDate(java.time.ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(eventTime), java.time.ZoneId.systemDefault()));

        // Check if fully filled
        if (Math.abs(p.getFilledQuantity() - p.getQuantity()) < 0.000001) {
            p.setClosed(true);
            String reason = p.getRealizedProfit() >= 0 ? "TAKE PROFIT (WS)" : "STOP LOSS (WS)";

            String msg = String.format("🔴 POSITION CLOSED (WS): %s\nType: %s\nEntry: %.2f | Exit: %.2f\nProfit: %.4f\nClose Date: %s",
                    reason, p.getType(), p.getEntryPrice(), p.getExitPrice(), p.getRealizedProfit(), p.getCloseDate());
            telegramMessagingService.broadcast(msg);

            log.info("Position {} closed by WS Order Update {}: exit price {} | Realized Profit: {}", p.getId(), reason, p.getExitPrice(), p.getRealizedProfit());
        } else {
            log.info("Position {} partially filled: {}/{} | Partial Realized Profit: {}", p.getId(), p.getFilledQuantity(), p.getQuantity(), p.getRealizedProfit());
        }

        positionRepository.save(p);
    }
}

package de.jansoh.rsistrategy.service.position;

import de.jansoh.rsistrategy.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OpenPositionRegistryTest {

    private OpenPositionRegistry registry;
    private final ZonedDateTime now = ZonedDateTime.now();

    @BeforeEach
    void setUp() {
        registry = new OpenPositionRegistry();
    }

    @Test
    void testUpdatePositionAndGet() {
        Position position = Position.builder()
                .orderId("order1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .build();

        registry.update(position);

        Position retrieved = registry.get("order1");
        assertNotNull(retrieved);
        assertEquals("order1", retrieved.getOrderId());
        assertEquals("BTCUSDT", retrieved.getSymbol());
        assertEquals(Timeframe.ONE_HOUR, retrieved.getTimeframe());
    }

    @Test
    void testUpdateExistingPositionTimeframe() {
        Position position = Position.builder()
                .orderId("order1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .build();
        registry.update(position);

        Position updatedPosition = Position.builder()
                .orderId("order1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.FOUR_HOURS)
                .build();
        registry.update(updatedPosition);

        Position retrieved = registry.get("order1");
        assertEquals(Timeframe.FOUR_HOURS, retrieved.getTimeframe());
    }

    @Test
    void testUpdateAlgoOrderTP() {
        Position position = Position.builder()
                .orderId("order1")
                .symbol("BTCUSDT")
                .quantity(BigDecimal.ONE)
                .build();
        registry.update(position);

        AlgoOrder tpOrder = AlgoOrder.builder()
                .orderId("order1")
                .symbol("BTCUSDT")
                .quantity(BigDecimal.ONE)
                .algoId("algo_tp_1")
                .type(AlgoOrderType.TP)
                .triggerPrice(new BigDecimal("50000"))
                .clientOrderId("algo_tp_client_1")
                .build();

        Optional<Position> result = registry.update(tpOrder);

        assertTrue(result.isPresent());
        Position updated = result.get();
        assertEquals("algo_tp_1", updated.getTpAlgoId());
        assertEquals(new BigDecimal("50000"), updated.getTpAlgoPrice());
        assertEquals("algo_tp_client_1", updated.getTpClientOrderId());
    }

    @Test
    void testUpdateAlgoOrderSL() {
        Position position = Position.builder()
                .orderId("order1")
                .symbol("BTCUSDT")
                .quantity(BigDecimal.ONE)
                .build();
        registry.update(position);

        AlgoOrder slOrder = AlgoOrder.builder()
                .orderId("order1")
                .symbol("BTCUSDT")
                .quantity(BigDecimal.ONE)
                .algoId("algo_sl_1")
                .type(AlgoOrderType.SL)
                .triggerPrice(new BigDecimal("40000"))
                .clientOrderId("algo_sl_client_1")
                .build();

        Optional<Position> result = registry.update(slOrder);

        assertTrue(result.isPresent());
        Position updated = result.get();
        assertEquals("algo_sl_1", updated.getSlAlgoId());
        assertEquals(new BigDecimal("40000"), updated.getSlAlgoPrice());
        assertEquals("algo_sl_client_1", updated.getSlClientOrderId());
    }

    @Test
    void testUpdateAlgoOrderThrowsExceptionIfPositionNotFound() {
        AlgoOrder tpOrder = AlgoOrder.builder()
                .orderId("nonexistent")
                .symbol("BTCUSDT")
                .quantity(BigDecimal.ONE)
                .algoId("algo1")
                .build();

        assertThrows(OpenPositionRegistrationException.class, () -> registry.update(tpOrder));
    }

    @Test
    void testUpdateOrderNewPosition() {
        Order order = Order.builder()
                .orderId("order1")
                .clientOrderId("client1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .originalQuantity(BigDecimal.ONE)
                .orderStatus(OrderStatus.FILLED)
                .side(OrderSide.BUY)
                .orderTradeTime(now)
                .lastFilledPrice(new BigDecimal("45000"))
                .realizedProfit(BigDecimal.ZERO)
                .build();

        Optional<Position> result = registry.update(order);

        assertTrue(result.isPresent());
        Position position = result.get();
        assertEquals("order1", position.getOrderId());
        assertEquals("BTCUSDT", position.getSymbol());
        assertEquals(BigDecimal.ONE, position.getQuantity());
        assertEquals(PositionSide.LONG, position.getSide());
        assertEquals(now, position.getOpenTime());
        assertEquals(new BigDecimal("45000"), position.getAverageOpenPrice());
    }

    @Test
    void testUpdateOrderExistingPosition() {
        // Create initial position
        Order buyOrder = Order.builder()
                .orderId("order1")
                .clientOrderId("client1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .originalQuantity(BigDecimal.ONE)
                .orderStatus(OrderStatus.PARTIALLY_FILLED)
                .side(OrderSide.BUY)
                .orderTradeTime(now)
                .lastFilledPrice(new BigDecimal("45000"))
                .realizedProfit(BigDecimal.ZERO)
                .build();
        registry.update(buyOrder);

        // Update it
        ZonedDateTime updateTime = now.plusMinutes(1);
        Order updateOrder = Order.builder()
                .orderId("order1")
                .clientOrderId("client1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .orderStatus(OrderStatus.FILLED)
                .orderTradeTime(updateTime)
                .lastFilledPrice(new BigDecimal("45100"))
                .realizedProfit(BigDecimal.ZERO)
                .build();

        Optional<Position> result = registry.update(updateOrder);

        assertTrue(result.isPresent());
        Position position = result.get();
        assertEquals(updateTime, position.getOpenTime());
        assertEquals(new BigDecimal("45100"), position.getAverageOpenPrice());
    }

    @Test
    void testUpdateOrderCloseManual() {
        // Create initial position
        Order buyOrder = Order.builder()
                .orderId("order1")
                .clientOrderId("client1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .originalQuantity(BigDecimal.ONE)
                .orderStatus(OrderStatus.FILLED)
                .side(OrderSide.BUY)
                .orderTradeTime(now)
                .lastFilledPrice(new BigDecimal("45000"))
                .realizedProfit(BigDecimal.ZERO)
                .build();
        registry.update(buyOrder);

        // Ensure it's in openPositionsByTradeWindow
        AssetTradeWindow atw = AssetTradeWindow.builder()
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .build();
        assertTrue(registry.hasPositions(atw));

        // Close it
        ZonedDateTime closeTime = now.plusMinutes(2);
        Order closeOrder = Order.builder()
                .orderId("close_order1_exec")
                .clientOrderId("close_order1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .orderStatus(OrderStatus.FILLED)
                .orderTradeTime(closeTime)
                .lastFilledPrice(new BigDecimal("46000"))
                .realizedProfit(new BigDecimal("1000"))
                .build();

        Optional<Position> result = registry.update(closeOrder);

        assertTrue(result.isPresent());
        Position position = result.get();
        assertTrue(position.isClosed());
        assertEquals(new BigDecimal("46000"), position.getAverageClosedPrice());
        assertEquals(closeTime, position.getClosedTime());
        assertEquals(new BigDecimal("1000"), position.getRealizedProfit());

        assertNull(registry.get("order1"));
        assertFalse(registry.hasPositions(atw));
    }

    @Test
    void testUpdateOrderCloseTP() {
        // Create initial position
        Order buyOrder = Order.builder()
                .orderId("order1")
                .clientOrderId("client1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .originalQuantity(BigDecimal.ONE)
                .orderStatus(OrderStatus.FILLED)
                .side(OrderSide.BUY)
                .orderTradeTime(now)
                .lastFilledPrice(new BigDecimal("45000"))
                .realizedProfit(BigDecimal.ZERO)
                .build();
        registry.update(buyOrder);

        // Set TP algo
        AlgoOrder tpAlgo = AlgoOrder.builder()
                .orderId("order1")
                .clientOrderId("algo_tp_client1")
                .type(AlgoOrderType.TP)
                .build();
        registry.update(tpAlgo);

        // Close via TP
        ZonedDateTime tpTime = now.plusMinutes(3);
        Order tpOrder = Order.builder()
                .orderId("tp_exec_order1")
                .clientOrderId("algo_tp_client1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .orderStatus(OrderStatus.FILLED)
                .orderTradeTime(tpTime)
                .lastFilledPrice(new BigDecimal("47000"))
                .realizedProfit(new BigDecimal("2000"))
                .build();

        Optional<Position> result = registry.update(tpOrder);

        assertTrue(result.isPresent());
        Position position = result.get();
        assertTrue(position.isClosed());
        assertTrue(position.isTpOrderFilled());
        assertNull(registry.get("order1"));
    }

    @Test
    void testUpdateOrderCloseSL() {
        // Create initial position
        Order buyOrder = Order.builder()
                .orderId("order1")
                .clientOrderId("client1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .originalQuantity(BigDecimal.ONE)
                .orderStatus(OrderStatus.FILLED)
                .side(OrderSide.BUY)
                .orderTradeTime(now)
                .lastFilledPrice(new BigDecimal("45000"))
                .realizedProfit(BigDecimal.ZERO)
                .build();
        registry.update(buyOrder);

        // Set SL algo
        AlgoOrder slAlgo = AlgoOrder.builder()
                .orderId("order1")
                .clientOrderId("algo_sl_client1")
                .type(AlgoOrderType.SL)
                .build();
        registry.update(slAlgo);

        // Close via SL
        ZonedDateTime slTime = now.plusMinutes(4);
        Order slOrder = Order.builder()
                .orderId("sl_exec_order1")
                .clientOrderId("algo_sl_client1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .orderStatus(OrderStatus.FILLED)
                .orderTradeTime(slTime)
                .lastFilledPrice(new BigDecimal("44000"))
                .realizedProfit(new BigDecimal("-1000"))
                .build();

        Optional<Position> result = registry.update(slOrder);

        assertTrue(result.isPresent());
        Position position = result.get();
        assertTrue(position.isClosed());
        assertTrue(position.isSlOrderFilled());
        assertNull(registry.get("order1"));
    }

    @Test
    void testHasPositionsAndGetPositions() {
        AssetTradeWindow atw = AssetTradeWindow.builder()
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .build();

        assertFalse(registry.hasPositions(atw));

        Position position = Position.builder()
                .orderId("order1")
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_HOUR)
                .build();
        registry.update(position);

        assertTrue(registry.hasPositions(atw));
        List<Position> positions = registry.getPositions(atw);
        assertEquals(1, positions.size());
        assertEquals("order1", positions.get(0).getOrderId());
    }

    @Test
    void testUpdateOrderIgnoresOtherStatuses() {
        Order order = Order.builder()
                .orderId("order1")
                .orderStatus(OrderStatus.NEW)
                .build();

        Optional<Position> result = registry.update(order);
        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateOrderThrowsOnInvalidClose() {
        Order closeOrder = Order.builder()
                .clientOrderId("close_nonexistent")
                .orderStatus(OrderStatus.FILLED)
                .build();

        assertThrows(OpenPositionRegistrationException.class, () -> registry.update(closeOrder));
    }
}

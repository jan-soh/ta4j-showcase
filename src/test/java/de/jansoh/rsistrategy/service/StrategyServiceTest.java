package de.jansoh.rsistrategy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class StrategyServiceTest {

    @Mock
    private BinanceApiService binanceApiService;

    private StrategyService strategyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // We only need StrategyService and its dependency BinanceApiService for this test
        strategyService = new StrategyService(binanceApiService, null, null, null, null, null, null, null, null);
        
        // Inject values normally provided by @Value
        ReflectionTestUtils.setField(strategyService, "sizePercentage", 5.0);
        ReflectionTestUtils.setField(strategyService, "commissionAsset", "USDT");
    }

    @Test
    void testCalculateQuantity() {
        // Setup mock balance
        List<Map<String, Object>> balances = new ArrayList<>();
        Map<String, Object> usdtBalance = new HashMap<>();
        usdtBalance.put("asset", "USDT");
        usdtBalance.put("balance", "1000.0");
        balances.add(usdtBalance);

        when(binanceApiService.getBalance()).thenReturn(balances);

        // Formula: (balance / currentPrice) * (percentage / 100)
        // (1000 / 60000) * (5 / 100) = 0.016666... * 0.05 = 0.0008333...
        
        double currentPrice = 60000.0;
        double expectedQuantity = (1000.0 / 60000.0) * (5.0 / 100.0);

        // Access private method via reflection
        double actualQuantity = (double) ReflectionTestUtils.invokeMethod(strategyService, "calculateQuantity", currentPrice);

        assertEquals(expectedQuantity, actualQuantity, 0.000001);
    }

    @Test
    void testCalculateQuantity_BalanceNotFound() {
        when(binanceApiService.getBalance()).thenReturn(new ArrayList<>());

        double currentPrice = 60000.0;
        double actualQuantity = (double) ReflectionTestUtils.invokeMethod(strategyService, "calculateQuantity", currentPrice);

        assertEquals(0.01, actualQuantity);
    }
}

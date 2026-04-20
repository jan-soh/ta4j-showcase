package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Disabled
class BinanceApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BinanceApiService binanceApiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void placeOrder() {

        String symbol = "BTCUSDT";
        String side = "BUY";
        String type = "MARKET";
        long timestamp = System.currentTimeMillis();
        String quantity = "0.01";

        BinanceOrderRequest orderRequest = BinanceOrderRequest.builder()
                .symbol(symbol)
                .side(side)
                .type(type)
                .timestamp(timestamp)
                .quantity(quantity)
                .build();

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("orderId", "12345");

        when(restTemplate.postForObject(eq("https://demo-fapi.binance.com/fapi/v1/order"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        Map<String, Object> result = binanceApiService.placeOrder(orderRequest);

        assertNotNull(result);
        assertEquals("12345", result.get("orderId"));
    }
}
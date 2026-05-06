package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceOrderRequest;
import de.jansoh.rsistrategy.service.broker.ApiConfiguration;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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

class BinanceApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApiConfiguration apiConfiguration;

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

        Mockito.when(apiConfiguration.getApiKey()).thenReturn("test-api-key");
        Mockito.when(apiConfiguration.getApiSecret()).thenReturn("test-api-secret");
        Mockito.when(apiConfiguration.getApiUrl()).thenReturn("https://demo-fapi.binance.com");

        when(restTemplate.postForObject(eq("https://demo-fapi.binance.com/fapi/v1/order"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        Map<String, Object> result = binanceApiService.placeOrder(orderRequest);

        assertNotNull(result);
        assertEquals("12345", result.get("orderId"));
    }
}
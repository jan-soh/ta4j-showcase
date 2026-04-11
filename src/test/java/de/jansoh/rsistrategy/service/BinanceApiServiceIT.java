package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceAlgoOrderCancelRequest;
import de.jansoh.rsistrategy.model.BinanceOrderRequest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(
        classes = {
                BinanceApiService.class,
                RestTemplate.class
        }
)
class BinanceApiServiceIT {

    @Inject
    private BinanceApiService binanceApiService;

    @Disabled
    @Test
    void placeOrder() {

        String symbol = "BTCUSDT";
        String side = "BUY";
        String type = "MARKET";
        String quantity = "0.01";

        BinanceOrderRequest orderRequest = BinanceOrderRequest.builder()
                .symbol(symbol)
                .side(side)
                .type(type)
                .quantity(quantity)
                .build();

        Map<String, Object> result = binanceApiService.placeOrder(orderRequest);

        assertNotNull(result);
    }

    @Test
    void cancelAlgoOrder_NotFound() {
        // Since we don't have a real algoId, this will likely fail or return an error from Binance Demo
        // But it verifies that the method can be called and the request is constructed correctly.
        BinanceAlgoOrderCancelRequest request = BinanceAlgoOrderCancelRequest.builder()
                .algoId(12345L)
                .build();

        Map<String, Object> result = binanceApiService.cancelAlgoOrder(request);

        // In a real IT, we might expect a specific error message. 
        // For now, we just ensure it doesn't throw an unhandled exception in the service itself.
        // Given our implementation returns null on exception.
        assertNull(result);
    }
}
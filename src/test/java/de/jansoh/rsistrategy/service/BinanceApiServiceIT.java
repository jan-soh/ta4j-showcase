package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceOrderRequest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
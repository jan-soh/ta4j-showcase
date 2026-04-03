package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceOrderRequest;
import jakarta.inject.Inject;
import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class BinanceApiServiceIT {

    @Inject
    private BinanceApiService binanceApiService;

    @Disabled
    @Test
    void placeOrder() {
        // Ensure we are using the demo API for tests
        binanceApiService.setRealApi(false);

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
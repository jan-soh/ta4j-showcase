package de.jansoh.rsistrategy.service;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {
                PositionService.class,
                BinanceApiService.class,
                RestTemplate.class
        }
)
class PositionServiceIT {

    @Inject
    PositionService positionService;

    @Disabled
    @Test
    void createPositionWithTpSl() {

        Map<String, Object> result = positionService.createPositionWithTpSl("BTCUSDT", "LONG", "0.01", 67000, 66500);
        assertNotNull(result);
    }
}
package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Disabled
@Profile("test")
@SpringBootTest(classes = {PrecisionService.class, BinanceApiService.class, RestTemplate.class})
class PrecisionServiceTestIT {

    @Autowired
    PrecisionService precisionService;

    @Test
    void getPrecision() {

        Precision btcPrecision = precisionService.getPrecision("BTCUSDT");
        Precision ethPrecision = precisionService.getPrecision("ETHUSDT");
        Precision bnbPrecision = precisionService.getPrecision("BNBUSDT");

        Assertions.assertEquals("BTCUSDT", btcPrecision.getSymbol());
        Assertions.assertEquals(2, btcPrecision.getPricePrecision());
        Assertions.assertEquals(4, btcPrecision.getQuantityPrecision());

        Assertions.assertEquals("ETHUSDT", ethPrecision.getSymbol());
        Assertions.assertEquals(2, ethPrecision.getPricePrecision());
        Assertions.assertEquals(3, ethPrecision.getQuantityPrecision());

        Assertions.assertEquals("BNBUSDT", bnbPrecision.getSymbol());
        Assertions.assertEquals(3, bnbPrecision.getPricePrecision());
        Assertions.assertEquals(2, bnbPrecision.getQuantityPrecision());
    }
}
package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmaCrossConfigurationFactoryTest {

    private final EmaCrossConfigurationFactory factory = new EmaCrossConfigurationFactory();

    @Test
    void testCreateWithEthUsdtConfig() {
        String fileName = "EmaCrossConfiguration-ethusdt_15m.json";
        EmaCrossConfiguration config = factory.create(fileName);

        assertNotNull(config);
        assertEquals("ETHUSDT", config.getAssetTradeWindow().getSymbol());
        assertEquals(1.8, config.getVolMultiplier());
        assertEquals(9, config.getVolAvgPeriod());
        assertEquals(1.6, config.getEma200DistPerc());
        assertEquals(1.4, config.getSlMultiplier());
        assertNotNull(config.getEntryDate());
    }

    @Test
    void testCreateWithBnbUsdtConfig() {
        String fileName = "EmaCrossConfiguration-bnbusdt_15m.json";
        EmaCrossConfiguration config = factory.create(fileName);
        assertNotNull(config);
        assertEquals("BNBUSDT", config.getAssetTradeWindow().getSymbol());
    }

    @Test
    void testCreateWithBtcUsdtConfig() {
        String fileName = "EmaCrossConfiguration-test.json";
        EmaCrossConfiguration config = factory.create(fileName);
        assertNotNull(config);
        assertEquals("BTCUSDT", config.getAssetTradeWindow().getSymbol());
    }

    @Test
    void testCreateWithMaxTradesConfig() {
        String fileName = "EmaCrossConfiguration-test-max-trades.json";
        EmaCrossConfiguration config = factory.create(fileName);
        assertNotNull(config);
    }

    @Test
    void testCreateWithInvalidFile() {
        assertThrows(RuntimeException.class, () -> factory.create("non-existent.json"));
    }
}

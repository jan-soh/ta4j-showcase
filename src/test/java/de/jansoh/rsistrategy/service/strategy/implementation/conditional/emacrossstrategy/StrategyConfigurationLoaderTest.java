package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class StrategyConfigurationLoaderTest {

    @Test
    public void testLoadEmaCrossConfiguration() {
        EmaCrossConfiguration config = new EmaCrossConfiguration();

        // Values from EmaCrossConfiguration-btcusdt_15m.json
        assertEquals(20, config.getEma20Length());
        assertEquals(50, config.getEma50Length());
        assertEquals(200, config.getEma200Length());
        assertTrue(config.isUseEma200Filter());
        assertFalse(config.isUseRsiFilter());
        assertEquals(50, config.getRsiThreshold());
        assertEquals(2.0, config.getVolMultiplier());
        assertEquals(1.5, config.getEma200DistPerc());
        assertEquals(LocalDate.of(2025, 9, 4), config.getEntryDate());
    }

    @StrategyProperties("test")
    public static class TestConfig {
        private String name;

        public TestConfig() {
            StrategyConfigurationLoader.populate(this);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void testLoadMissingConfiguration() {
        // TestConfig-test.json does not exist
        TestConfig config = new TestConfig();
        assertNotNull(config);
        assertNull(config.getName());
    }
}

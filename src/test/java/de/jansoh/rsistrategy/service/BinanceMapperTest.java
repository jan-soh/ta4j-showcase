package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.service.broker.binance.BinanceMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BinanceMapperTest {

    @Test
    void toFormData_FormatsBigDecimalWithPrecision() {
        TestPojo pojo = new TestPojo();
        pojo.setAmount(new BigDecimal("123.456789"));

        String formData = BinanceMapper.toFormData(pojo);

        // Expected with default precision 4: 123.4568 (rounded HALF_UP) or 123.4567 (cut-off)
        // Let's see what currently happens (it will probably be 123.456789)
        System.out.println("[DEBUG_LOG] FormData: " + formData);

        // If precision is 4, we expect 4 decimal places.
        // For 123.456789 it would be 123.4568
        assertTrue(formData.contains("amount=123.4568"), "FormData should contain amount=123.4568, but was: " + formData);
    }

    public static class TestPojo {
        private BigDecimal amount;

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}

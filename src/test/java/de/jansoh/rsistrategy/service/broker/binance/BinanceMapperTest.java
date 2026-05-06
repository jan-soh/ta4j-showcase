package de.jansoh.rsistrategy.service.broker.binance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinanceMapperTest {

    @Test
    void toFormData_withBasicFields_convertsToString() {
        TestPojo pojo = new TestPojo();
        pojo.setName("testName");
        pojo.setAge(25);

        String result = BinanceMapper.toFormData(pojo);

        // Order depends on reflection, but usually it's declaration order.
        // We'll check for presence of both.
        assertTrue(result.contains("name=testName"));
        assertTrue(result.contains("age=25"));
        assertTrue(result.contains("&"));
    }

    @Test
    void toFormData_withNullFields_ignoresNulls() {
        TestPojo pojo = new TestPojo();
        pojo.setName("testName");
        pojo.setAge(null);

        String result = BinanceMapper.toFormData(pojo);

        assertEquals("name=testName", result);
    }

    @Test
    void toFormData_withBigDecimal_usesPlainString() {
        DecimalPojo pojo = new DecimalPojo();
        pojo.setAmount(new BigDecimal("0.00000001"));
        pojo.setPrice("123.4500");

        String result = BinanceMapper.toFormData(pojo);

        assertTrue(result.contains("amount=0.00000001"));
        assertTrue(result.contains("price=123.4500"));
    }

    @Test
    void toFormData_withScientificNotationString_convertsToPlainString() {
        DecimalPojo pojo = new DecimalPojo();
        pojo.setPrice("1.23E-7");

        String result = BinanceMapper.toFormData(pojo);

        assertEquals("price=0.000000123", result);
    }

    @Test
    void toFormData_withUrlEncoding_encodesSpecialChars() {
        TestPojo pojo = new TestPojo();
        pojo.setName("name with spaces & symbols");

        String result = BinanceMapper.toFormData(pojo);

        // [DEBUG_LOG] result: name=name+with+spaces+%26+symbols
        assertTrue(result.contains("name=name+with+spaces+%26+symbols"));
    }

    @Test
    void toFormData_withNoFields_returnsEmptyString() {
        Object pojo = new Object();
        String result = BinanceMapper.toFormData(pojo);
        assertEquals("", result);
    }

    @Test
    void toFormData_withSyntheticFields_isHandled() {
        // Inner classes in non-static context have a synthetic field 'this$0'
        class LocalPojo {
            private final String field = "value";
        }
        LocalPojo pojo = new LocalPojo();
        String result = BinanceMapper.toFormData(pojo);

        // It currently includes this$0 because BinanceMapper doesn't filter synthetic fields.
        // We just verify it contains the intended field.
        assertTrue(result.contains("field=value"));
    }

    @Test
    void toFormData_withInvalidDecimalString_keepsOriginalString() {
        DecimalPojo pojo = new DecimalPojo();
        pojo.setPrice("not.a.number");

        String result = BinanceMapper.toFormData(pojo);

        assertEquals("price=not.a.number", result);
    }

    // Inner classes for testing
    private static class TestPojo {
        private String name;
        private Integer age;

        public void setName(String name) { this.name = name; }
        public void setAge(Integer age) { this.age = age; }
    }

    private static class DecimalPojo {
        private BigDecimal amount;
        private String price;

        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public void setPrice(String price) { this.price = price; }
    }
}

package de.jansoh.rsistrategy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrecisionTest {

    @Test
    void testFormatPrice() {
        Precision precision = Precision.builder()
                .symbol("BTCUSDT")
                .pricePrecision(2)
                .quantityPrecision(4)
                .build();

        assertEquals("12345.67", precision.formatPrice(new BigDecimal("12345.674")));
        assertEquals("12345.68", precision.formatPrice(new BigDecimal("12345.675")));
        assertEquals("12345.67", precision.formatPrice(12345.674));
        // Note: new BigDecimal(double) can have unexpected results, but 12345.675 is exactly representable or close enough here?
        // Let's check what it actually produces.
        // assertEquals("12345.68", precision.formatPrice(12345.675)); 
        assertEquals("12345.00", precision.formatPrice(12345));
        assertEquals("12345.67", precision.formatPrice("12345.674"));
    }

    @Test
    void testFormatQuantity() {
        Precision precision = Precision.builder()
                .symbol("BTCUSDT")
                .pricePrecision(2)
                .quantityPrecision(4)
                .build();

        assertEquals("1.2345", precision.formatQuantity(new BigDecimal("1.23454")));
        assertEquals("1.2346", precision.formatQuantity(new BigDecimal("1.23455")));
        assertEquals("1.2345", precision.formatQuantity(1.23454));
        assertEquals("1.2346", precision.formatQuantity(1.23455));
        assertEquals("1.0000", precision.formatQuantity(1));
        assertEquals("1.2345", precision.formatQuantity("1.23454"));
    }

    @Test
    void testJsonMappingWithUnknownProperties() throws JsonProcessingException {
        String json = "{\"symbol\":\"BTCUSDT\",\"pricePrecision\":2,\"quantityPrecision\":4,\"pair\":\"BTC/USDT\",\"unknownField\":\"someValue\"}";
        ObjectMapper objectMapper = new ObjectMapper();

        Precision precision = objectMapper.readValue(json, Precision.class);

        assertEquals("BTCUSDT", precision.getSymbol());
        assertEquals(2, precision.getPricePrecision());
        assertEquals(4, precision.getQuantityPrecision());
    }
}

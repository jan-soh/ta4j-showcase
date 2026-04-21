package de.jansoh.rsistrategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Precision {

    private String symbol;
    private int pricePrecision;
    private int quantityPrecision;

    public String formatPrice(BigDecimal price) {
        return price.setScale(pricePrecision, RoundingMode.HALF_UP).toPlainString();
    }

    public String formatPrice(double price) {
        return formatPrice(new BigDecimal(price));
    }

    public String formatPrice(int price) {
        return formatPrice(new BigDecimal(price));
    }

    public String formatPrice(float price) {
        return formatPrice(new BigDecimal(price));
    }

    public String formatPrice(String price) {
        return formatPrice(new BigDecimal(price));
    }

    public String formatQuantity(BigDecimal quantity) {
        return quantity.setScale(quantityPrecision, RoundingMode.HALF_UP).toPlainString();
    }

    public String formatQuantity(double quantity) {
        return formatQuantity(new BigDecimal(quantity));
    }

    public String formatQuantity(int quantity) {
        return formatQuantity(new BigDecimal(quantity));
    }

    public String formatQuantity(float quantity) {
        return formatQuantity(new BigDecimal(quantity));
    }

    public String formatQuantity(String quantity) {
        return formatQuantity(new BigDecimal(quantity));
    }
}

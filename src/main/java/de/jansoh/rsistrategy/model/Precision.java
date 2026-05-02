package de.jansoh.rsistrategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The Precision class provides functionality to format price and quantity values
 * based on the specified precision levels. This is particularly useful in financial
 * or trading applications where consistent formatting is required for display or processing.
 * <p>
 * The class supports formatting price and quantity values from various data types
 * including BigDecimal, double, int, float, and String. It uses the
 * {@link RoundingMode#HALF_UP} rounding mode during formatting.
 * <p>
 * Fields:
 * - symbol: A string representing the symbol or identifier associated with the precision settings.
 * - pricePrecision: An integer specifying the number of decimal places for price formatting.
 * - quantityPrecision: An integer specifying the number of decimal places for quantity formatting.
 * <p>
 * Methods:
 * - formatPrice(BigDecimal): Formats the given price value according to the price precision.
 * - formatPrice(double): Converts the double price to BigDecimal and formats it.
 * - formatPrice(int): Converts the integer price to BigDecimal and formats it.
 * - formatPrice(float): Converts the float price to BigDecimal and formats it.
 * - formatPrice(String): Converts the String price to BigDecimal and formats it.
 * - formatQuantity(BigDecimal): Formats the given quantity value according to the quantity precision.
 * - formatQuantity(double): Converts the double quantity to BigDecimal and formats it.
 * - formatQuantity(int): Converts the integer quantity to BigDecimal and formats it.
 * - formatQuantity(float): Converts the float quantity to BigDecimal and formats it.
 * - formatQuantity(String): Converts the String quantity to BigDecimal and formats it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Precision {

    /**
     * Represents the trading symbol associated with the precision configurations.
     * This field is used to identify the symbol for which the price and quantity
     * precision settings apply.
     */
    private String symbol;

    /**
     * Represents the number of decimal places used for formatting the price values.
     * This variable determines the precision level for price-related operations
     * in the context of financial or trading calculations.
     */
    private int pricePrecision;

    /**
     * Specifies the precision level to be used for formatting quantities.
     * This determines the number of decimal places to retain when handling
     * quantity-related values in the system.
     */
    private int quantityPrecision;

    /**
     * Formats the given price by setting its scale to the predefined precision
     * and rounding it using HALF_UP rounding mode.
     *
     * @param price the price to be formatted as a BigDecimal
     * @return the formatted price as a plain string
     */
    public String formatPrice(BigDecimal price) {
        return price.setScale(pricePrecision, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Converts the provided price from a double value into a formatted string representation.
     * The method delegates to the {@code formatPrice(BigDecimal)} method for applying precision
     * and rounding rules to the given price.
     *
     * @param price the price to be formatted as a double
     * @return the formatted price as a plain string
     */
    public String formatPrice(double price) {
        return formatPrice(new BigDecimal(price));
    }

    /**
     * Converts the provided price from an integer value into a formatted string representation.
     * The method delegates to the {@code formatPrice(BigDecimal)} method for applying precision
     * and rounding rules to the given price.
     *
     * @param price the price to be formatted as an integer
     * @return the formatted price as a plain string
     */
    public String formatPrice(int price) {
        return formatPrice(new BigDecimal(price));
    }

    /**
     * Converts the provided price from a float value into a formatted string representation.
     * The method delegates to the {@code formatPrice(BigDecimal)} method for applying precision
     * and rounding rules to the given price.
     *
     * @param price the price to be formatted as a float
     * @return the formatted price as a plain string
     */
    public String formatPrice(float price) {
        return formatPrice(new BigDecimal(price));
    }

    /**
     * Converts the provided price from a string value into a formatted string representation.
     * The method delegates to the {@code formatPrice(BigDecimal)} method for applying precision
     * and rounding rules to the given price.
     *
     * @param price the price to be formatted as a string
     * @return the formatted price as a plain string
     */
    public String formatPrice(String price) {
        return formatPrice(new BigDecimal(price));
    }

    /**
     * Formats the given quantity by setting its scale to the predefined precision
     * and rounding it using HALF_UP rounding mode.
     *
     * @param quantity the quantity to be formatted as a BigDecimal
     * @return the formatted quantity as a plain string
     */
    public String formatQuantity(BigDecimal quantity) {
        return quantity.setScale(quantityPrecision, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Converts the provided quantity from a double value into a formatted string representation.
     * The method delegates to the {@code formatQuantity(BigDecimal)} method for applying precision
     * and rounding rules to the given quantity.
     *
     * @param quantity the quantity to be formatted as a double
     * @return the formatted quantity as a plain string
     */
    public String formatQuantity(double quantity) {
        return formatQuantity(new BigDecimal(quantity));
    }

    /**
     * Converts the provided quantity from an integer value into a formatted string representation.
     * The method delegates to the {@code formatQuantity(BigDecimal)} method for applying precision
     * and rounding rules to the given quantity.
     *
     * @param quantity the quantity to be formatted as an integer
     * @return the formatted quantity as a plain string
     */
    public String formatQuantity(int quantity) {
        return formatQuantity(new BigDecimal(quantity));
    }

    /**
     * Converts the provided quantity from a float value into a formatted string representation.
     * The method delegates to the {@code formatQuantity(BigDecimal)} method for applying precision
     * and rounding rules to the given quantity.
     *
     * @param quantity the quantity to be formatted as a float
     * @return the formatted quantity as a plain string
     */
    public String formatQuantity(float quantity) {
        return formatQuantity(new BigDecimal(quantity));
    }

    /**
     * Converts the provided quantity from a string value into a formatted string representation.
     * The method delegates to the {@code formatQuantity(BigDecimal)} method for applying
     * precision and rounding rules to the given quantity.
     *
     * @param quantity the quantity to be formatted as a string
     * @return the formatted quantity as a plain string
     */
    public String formatQuantity(String quantity) {
        return formatQuantity(new BigDecimal(quantity));
    }
}

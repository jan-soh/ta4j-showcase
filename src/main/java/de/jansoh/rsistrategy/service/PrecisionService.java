package de.jansoh.rsistrategy.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

//@Service
public class PrecisionService {

    private final BinanceApiService binanceApiService;

    private final Map<String, Integer> precisionMap = new HashMap<>();

    @Getter
    @Setter
    private String defaultSymbol = "BTCUSDT";

    @PostConstruct
    public void init() {
        Map<String, Object> exchangeInfo = binanceApiService.getExchangeInfo();
        Map<String, Object> symbols = (Map<String, Object>) exchangeInfo.get("symbols");

        for (Map<String, Object> symbol : (Iterable<Map<String, Object>>) symbols.get("symbol")) {
            String symbolName = (String) symbol.get("symbol");
            int decimalPlaces = (int) symbol.get("pricePrecision");
            precisionMap.put(symbolName, decimalPlaces);
        }
    }

    public PrecisionService(BinanceApiService binanceApiService) {
        this.binanceApiService = binanceApiService;
    }

    public int getPrecision(String symbol) {
        return precisionMap.getOrDefault(symbol, 0);
    }

    /**
     * Retrieves the price precision for the default trading symbol.
     * The default symbol is configured through the {@code defaultSymbol} property.
     *
     * @return the precision value (number of decimal places) used for the default symbol,
     * or 0 if the precision for the default symbol is not found.
     */
    public int getPrecision() {
        return getPrecision(defaultSymbol);
    }

    public BigDecimal toPrecision(String value) {
        return new BigDecimal(value).setScale(getPrecision(), RoundingMode.HALF_UP);
    }

    public BigDecimal toPrecision(int value) {
        return new BigDecimal(value).setScale(getPrecision(), RoundingMode.HALF_UP);
    }

    public BigDecimal toPrecision(float value) {
        return new BigDecimal(value).setScale(getPrecision(), RoundingMode.HALF_UP);
    }

    public BigDecimal toPrecision(double value) {
        return new BigDecimal(value).setScale(getPrecision(), RoundingMode.HALF_UP);
    }

    public BigDecimal toPrecision(BigDecimal value) {
        return value.setScale(getPrecision(), RoundingMode.HALF_UP);
    }
}

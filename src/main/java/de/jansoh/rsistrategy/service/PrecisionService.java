package de.jansoh.rsistrategy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.Precision;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrecisionService {

    private final BinanceApiService binanceApiService;

    private final Map<String, Precision> precisionMap = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        Map<String, Object> exchangeInfo = binanceApiService.getExchangeInfo();
        Precision[] symbols = objectMapper.convertValue(exchangeInfo.get("symbols"), Precision[].class);

        for (Precision precision : symbols) {
            precisionMap.put(precision.getSymbol(), precision);
        }

        log.info("----- PrecisionService ----- Initialized precision map with {} entries", precisionMap.size());
    }

    public Precision getPrecision(String symbol) {
        if (!precisionMap.containsKey(symbol)) {
            throw new PrecisionNotFoundForSymbolException("Precision not found for symbol: " + symbol);
        }
        return precisionMap.get(symbol);
    }
}

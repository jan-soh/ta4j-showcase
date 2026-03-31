package de.jansoh.rsistrategy.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.ArrayList;

@Service
public class BinanceApiService {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://fapi.binance.com/fapi/v1/klines";

    public BinanceApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch klines for a symbol and interval.
     * @param symbol Symbol like BTCUSDT
     * @param interval Interval like 1m
     * @param limit Maximum data points
     * @return List of klines (each kline is an array of objects)
     */
    public List<Object[]> getKlines(String symbol, String interval, Integer limit) {
        String url = String.format("%s?symbol=%s&interval=%s", BASE_URL, symbol, interval);
        if (limit != null) {
            url += "&limit=" + limit;
        }
        // Binance returns a JSON array of arrays
        List rawList = restTemplate.getForObject(url, List.class);
        List<Object[]> result = new ArrayList<>();
        if (rawList != null) {
            for (Object item : rawList) {
                if (item instanceof List) {
                    result.add(((List) item).toArray());
                }
            }
        }
        return result;
    }
}

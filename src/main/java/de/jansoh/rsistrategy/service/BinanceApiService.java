package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceOrderRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BinanceApiService {

    private final RestTemplate restTemplate;

    @Value("${binance.demo.api.key}")
    private String apiKey;

    @Value("${binance.demo.api.secret}")
    private String apiSecret;

    private boolean isRealApi = false;

    private static final String REAL_BASE_URL = "https://fapi.binance.com";
    private static final String DEMO_BASE_URL = "https://demo-fapi.binance.com";

    private String getBaseUrl() {
        return isRealApi ? REAL_BASE_URL : DEMO_BASE_URL;
    }

    public void setRealApi(boolean realApi) {
        this.isRealApi = realApi;
    }

    public BinanceApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Set leverage for a symbol.
     */
    public void setLeverage(String symbol, int leverage) {
        long timestamp = System.currentTimeMillis();
        String formData = String.format("symbol=%s&leverage=%d&timestamp=%d",
                symbol, leverage, timestamp);

        String signature = sign(formData, apiSecret);
        String body = formData + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(getBaseUrl() + "/fapi/v1/leverage", entity, Map.class);
        } catch (Exception e) {
            System.err.println("Error setting leverage: " + e.getMessage());
        }
    }

    /**
     * Set margin type for a symbol.
     */
    public void setMarginType(String symbol, String marginType) {
        long timestamp = System.currentTimeMillis();
        String formData = String.format("symbol=%s&marginType=%s&timestamp=%d",
                symbol, marginType, timestamp);

        String signature = sign(formData, apiSecret);
        String body = formData + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(getBaseUrl() + "/fapi/v1/marginType", entity, Map.class);
        } catch (Exception e) {
            System.err.println("Error setting margin type: " + e.getMessage());
        }
    }

    /**
     * Fetch klines for a symbol and interval.
     */
    public List<Object[]> getKlines(String symbol, String interval, Integer limit) {
        String url = String.format("%s/fapi/v1/klines?symbol=%s&interval=%s", getBaseUrl(), symbol, interval);
        if (limit != null) {
            url += "&limit=" + limit;
        }
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

    /**
     * Place an order on Binance Futures (Demo).
     */
    public Map<String, Object> placeOrder(BinanceOrderRequest orderRequest) {
        if (orderRequest.getTimestamp() == null) {
            orderRequest.setTimestamp(System.currentTimeMillis());
        }
        String formData = BinanceMapper.toFormData(orderRequest);
        String signature = sign(formData, apiSecret);
        String body = formData + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        // Set API key if provided. Even for demo, it's often needed as 'X-MBX-APIKEY'.
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.postForObject(getBaseUrl() + "/fapi/v1/order", entity, Map.class);
        } catch (Exception e) {
            System.err.println("Error placing order on " + (isRealApi ? "REAL" : "DEMO") + " API: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> placeOrder(String symbol, String side, String type, String quantity) {
        BinanceOrderRequest request = BinanceOrderRequest.builder()
                .symbol(symbol)
                .side(side)
                .type(type)
                .quantity(quantity)
                .build();
        return placeOrder(request);
    }

    private String sign(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign request", e);
        }
    }
}

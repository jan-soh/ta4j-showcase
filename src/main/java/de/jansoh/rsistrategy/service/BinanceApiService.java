package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceAlgoOrderRequest;
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
import java.util.List;
import java.util.Map;

@Service
public class BinanceApiService {

    private final RestTemplate restTemplate;

    @Value("${binance.demo.api.key}")
    private String apiKey;

    @Value("${binance.demo.api.secret}")
    private String apiSecret;

    private boolean isRealApi = false;
    private long lastCandleCloseTime = 0;

    private static final String REAL_BASE_URL = "https://fapi.binance.com";
    private static final String DEMO_BASE_URL = "https://demo-fapi.binance.com";

    public String getBaseUrl() {
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

    public long getLastCandleCloseTime() {
        return lastCandleCloseTime;
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
                    Object[] candle = ((List) item).toArray();
                    result.add(candle);
                    // Binance Kline format: [6] is Close time
                    if (candle.length > 6) {
                        long closeTime = Long.parseLong(candle[6].toString());
                        if (closeTime > lastCandleCloseTime) {
                            lastCandleCloseTime = closeTime;
                        }
                    }
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

    /**
     * Place an algo order on Binance Futures (e.g., SL/TP).
     */
    public Map<String, Object> placeAlgoOrder(BinanceAlgoOrderRequest algoOrderRequest) {
        if (algoOrderRequest.getTimestamp() == null) {
            algoOrderRequest.setTimestamp(System.currentTimeMillis());
        }
        String formData = BinanceMapper.toFormData(algoOrderRequest);
        String signature = sign(formData, apiSecret);
        String body = formData + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.postForObject(getBaseUrl() + "/fapi/v1/algoOrder", entity, Map.class);
        } catch (Exception e) {
            System.err.println("Error placing algo order on " + (isRealApi ? "REAL" : "DEMO") + " API: " + e.getMessage());
            return null;
        }
    }

    /**
     * Query algo order status.
     */
    public Map<String, Object> getAlgoOrder(String algoId) {
        long timestamp = System.currentTimeMillis();
        String query = String.format("algoId=%s&timestamp=%d", algoId, timestamp);
        String signature = sign(query, apiSecret);
        String url = String.format("%s/fapi/v1/algoOrder?%s&signature=%s", getBaseUrl(), query, signature);

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class).getBody();
        } catch (Exception e) {
            System.err.println("Error querying algo order: " + e.getMessage());
            return null;
        }
    }

    /**
     * Query regular order status.
     */
    public Map<String, Object> getOrder(String symbol, String orderId) {
        long timestamp = System.currentTimeMillis();
        String query = String.format("symbol=%s&orderId=%s&timestamp=%d", symbol, orderId, timestamp);
        String signature = sign(query, apiSecret);
        String url = String.format("%s/fapi/v1/order?%s&signature=%s", getBaseUrl(), query, signature);

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class).getBody();
        } catch (Exception e) {
            System.err.println("Error querying order: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cancel an algo order.
     */
    public Map<String, Object> cancelAlgoOrder(String algoId) {
        long timestamp = System.currentTimeMillis();
        String query = String.format("algoId=%s&timestamp=%d", algoId, timestamp);
        String signature = sign(query, apiSecret);
        String url = String.format("%s/fapi/v1/algoOrder?%s&signature=%s", getBaseUrl(), query, signature);

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.DELETE, entity, Map.class).getBody();
        } catch (Exception e) {
            System.err.println("Error cancelling algo order: " + e.getMessage());
            return null;
        }
    }

    /**
     * Start a new user data stream and return the listen key.
     */
    public String startUserDataStream() {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            Map<String, Object> response = restTemplate.postForObject(getBaseUrl() + "/fapi/v1/listenKey", entity, Map.class);
            return response != null ? response.get("listenKey").toString() : null;
        } catch (Exception e) {
            System.err.println("Error starting user data stream: " + e.getMessage());
            return null;
        }
    }

    /**
     * Keepalive a user data stream.
     */
    public void keepAliveUserDataStream() {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            restTemplate.put(getBaseUrl() + "/fapi/v1/listenKey", entity);
        } catch (Exception e) {
            System.err.println("Error keeping alive user data stream: " + e.getMessage());
        }
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

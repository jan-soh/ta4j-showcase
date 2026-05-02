package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.BinanceAlgoOrderCancelRequest;
import de.jansoh.rsistrategy.model.BinanceAlgoOrderRequest;
import de.jansoh.rsistrategy.model.BinanceOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service class for interacting with the Binance API, including Futures trading, market data,
 * user account management, and more. Provides functionality to interact with both real
 * and demo Binance environments.
 * <p>
 * This class handles API authentication, request signing, and manages RESTful
 * interactions with the Binance platform.
 */
@Slf4j
@Service
public class BinanceApiService {

    /**
     * A thread-safe, reusable instance of RestTemplate used for making HTTP requests.
     * This object facilitates communication with RESTful web services by providing
     * methods to send HTTP requests and handle responses.
     * <p>
     * It supports various HTTP methods such as GET, POST, PUT, DELETE, and more,
     * and can be configured with additional settings such as request interceptors,
     * error handlers, and message converters.
     */
    private final RestTemplate restTemplate;

    /**
     * The API key used for authenticating requests to the Binance API.
     * This key is typically provided through an external configuration and injected into the application.
     * It should be kept secure and not exposed publicly.
     */
    @Value("${binance.api.key}")
    private String apiKey;

    /**
     * The secret key used for authenticating API requests to the Binance platform.
     * This value is securely injected from application properties using the @Value annotation.
     * It is crucial to keep this key confidential to prevent unauthorized access.
     */
    @Value("${binance.api.secret}")
    private String apiSecret;

    /**
     * A flag that indicates whether to use the real Binance API or a simulated environment.
     * This value is injected from the application configuration using the property
     * "binance.use-real-api".
     * <p>
     * When set to {@code true}, the application interacts with the real Binance API.
     * When set to {@code false}, the application operates in a testing or simulated mode
     * where no real transactions or API calls are made.
     */
    @Value("${binance.use-real-api}")
    private boolean isRealApi;

    /**
     * The base URL for accessing Binance's REST API.
     * This constant specifies the endpoint for connecting to Binance's
     * full API services, used for performing secure and authenticated
     * requests to retrieve or manage data related to cryptocurrency
     * trading and account information.
     */
    private static final String REAL_BASE_URL = "https://fapi.binance.com";

    /**
     * The base URL for accessing the Binance demo API.
     * This constant is used to connect to the Binance demo environment,
     * allowing for testing without interacting with the live production API.
     */
    private static final String DEMO_BASE_URL = "https://demo-fapi.binance.com";

    /**
     * Retrieves the base URL to be used by the application, depending on
     * whether the real API or the demo API is being used.
     *
     * @return The base URL as a String. Returns REAL_BASE_URL if the real API
     * is active, otherwise returns DEMO_BASE_URL.
     */
    public String getBaseUrl() {
        return isRealApi ? REAL_BASE_URL : DEMO_BASE_URL;
    }

    /**
     * Constructor for the BinanceApiService class.
     *
     * @param restTemplate the RestTemplate instance used for making HTTP requests to the Binance API.
     */
    public BinanceApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieves exchange information from the API.
     * <p>
     * Makes a REST call to the endpoint that provides detailed information
     * about the exchange, such as trading pairs, rate limits, and other
     * exchange-wide metadata.
     *
     * @return A map containing exchange information, where the data structure
     * and keys depend on the API's response format.
     */
    public Map<String, Object> getExchangeInfo() {
        return restTemplate.getForObject(getBaseUrl() + "/fapi/v1/exchangeInfo", Map.class);
    }

    /**
     * Sets the leverage for a given trading symbol.
     *
     * @param symbol   the trading symbol for which the leverage is to be set
     * @param leverage the desired leverage value to be applied
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
            log.error("Error setting leverage.", e);
        }
    }

    /**
     * Sets the margin type for a specific symbol on the trading platform.
     *
     * @param symbol     the trading pair symbol for which the margin type is being set (e.g., "BTCUSDT").
     * @param marginType the type of margin to be set for the symbol (e.g., "ISOLATED" or "CROSSED").
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
            log.error("Error setting margin type.", e);
        }
    }

    /**
     * Fetches Kline (candlestick) data for a specific trading pair and interval from
     * the API, and returns it as a list of object arrays.
     *
     * @param symbol   The trading pair symbol (e.g., "BTCUSDT") for which to fetch candlestick data.
     * @param interval The interval of the candlesticks (e.g., "1m", "5m", "1h", etc.).
     * @param limit    The maximum number of candlestick records to fetch. If null, the default limit is applied.
     * @return A list of object arrays where each array represents a candlestick record.
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
                }
            }
        }
        return result;
    }

    /**
     * Places a new order on the Binance API and returns the server's response.
     *
     * @param orderRequest The request object containing order details such as symbol, side, type, quantity, and other parameters.
     *                     The timestamp will be automatically set if not provided.
     * @return A map containing the response from the Binance API. This includes details about the placed order
     * such as order ID, status, and other relevant information.
     * @throws BinanceApiServiceOrderException If there is an error while attempting to place the order.
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
            throw new BinanceApiServiceOrderException("Error placing order on " + (isRealApi ? "REAL" : "DEMO") + " API.", e);
        }
    }

    /**
     * Places an algorithmic order on the Binance platform by sending the given request payload.
     *
     * @param algoOrderRequest the request object containing all necessary data for placing the algorithmic order.
     *                         The timestamp will be automatically set if it's null.
     * @return a map containing the response from the Binance API, which typically includes details about the placed order.
     * @throws BinanceApiServiceOrderException if there is an error while placing the order.
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
            throw new BinanceApiServiceOrderException("Error placing algo order on " + (isRealApi ? "REAL" : "DEMO") + " API.", e);
        }
    }

    /**
     * Retrieves details of an algorithmic order using the provided algo ID.
     *
     * @param algoId The unique identifier of the algorithmic order to be retrieved.
     * @return A map containing the details of the algorithmic order. Returns null if an error occurs during the API call.
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
            log.error("Error querying algo order.", e);
            return null;
        }
    }

    /**
     * Retrieves the details of an order using the provided symbol and order ID.
     *
     * @param symbol  the trading pair symbol for which the order was placed (e.g., BTCUSDT)
     * @param orderId the unique identifier of the order to be retrieved
     * @return a map containing the details of the order, or null if an error occurs during the request
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
            log.error("Error querying order.", e);
            return null;
        }
    }

    /**
     * Cancels an algorithmic order on Binance using the provided request parameters.
     *
     * @param request a {@code BinanceAlgoOrderCancelRequest} object containing the necessary parameters
     *                for cancelling the algorithmic order. It includes the algoId, clientAlgoId,
     *                recvWindow, and timestamp.
     * @return a {@code Map<String, Object>} containing the response from Binance API after attempting
     * to cancel the order. Returns {@code null} if an error occurs during the request.
     */
    public Map<String, Object> cancelAlgoOrder(BinanceAlgoOrderCancelRequest request) {
        if (request.getTimestamp() == null) {
            request.setTimestamp(System.currentTimeMillis());
        }

        List<String> params = new ArrayList<>();
        if (request.getAlgoId() != null) params.add("algoId=" + request.getAlgoId());
        if (request.getClientAlgoId() != null) params.add("clientAlgoId=" + request.getClientAlgoId());
        if (request.getRecvWindow() != null) params.add("recvWindow=" + request.getRecvWindow());
        params.add("timestamp=" + request.getTimestamp());

        String query = String.join("&", params);
        String signature = sign(query, apiSecret);
        String url = String.format("%s/fapi/v1/algoOrder?%s&signature=%s", getBaseUrl(), query, signature);

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, HttpMethod.DELETE, entity, Map.class).getBody();
        } catch (Exception e) {
            log.error("Error cancelling algo order.", e);
            return null;
        }
    }

    /**
     * Cancels an algorithmic order based on the provided algorithmic order ID.
     *
     * @param algoId the unique identifier of the algorithmic order to be canceled
     * @return a map containing the response details of the cancellation request
     */
    public Map<String, Object> cancelAlgoOrder(String algoId) {
        BinanceAlgoOrderCancelRequest request = BinanceAlgoOrderCancelRequest.builder()
                .algoId(Long.parseLong(algoId))
                .build();
        return cancelAlgoOrder(request);
    }

    /**
     * Initiates a new user data stream and retrieves the listen key associated with it.
     * The listen key is used to maintain a WebSocket connection to a user data stream
     * endpoint for receiving account updates and other relevant events.
     *
     * @return the listen key for the user data stream if successful, or null in case of an error.
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
            log.error("Error starting user data stream.", e);
            return null;
        }
    }

    /**
     * Sends a PUT request to keep a user data stream alive.
     * This method is typically used to extend the lifetime of an active user data stream
     * by refreshing its validity, preventing it from expiring.
     * <p>
     * The method constructs an HTTP request with the appropriate headers,
     * including the API key, if available. It then makes a PUT request
     * to the designated endpoint of the API server.
     * Any errors encountered during the request are caught and logged.
     * <p>
     * Preconditions:
     * - The method assumes that an API key and base URL are properly configured.
     * <p>
     * Side effects:
     * - Updates the server to extend the validity of a specific user data stream.
     * <p>
     * Logs:
     * - If an exception occurs during the HTTP request, the error details are logged.
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
            log.error("Error keeping alive user data stream.", e);
        }
    }

    /**
     * Fetches the account balance by making an HTTP GET request to the corresponding API endpoint.
     * The method constructs the API request URL with a timestamp and a signature for authentication,
     * adds required headers, and processes the response to return the balance information.
     *
     * @return a list of maps, each map containing the balance details as key-value pairs,
     * or null if an error occurs while making the request.
     */
    public List<Map<String, Object>> getBalance() {
        long timestamp = System.currentTimeMillis();
        String query = String.format("timestamp=%d", timestamp);
        String signature = sign(query, apiSecret);
        String url = String.format("%s/fapi/v3/balance?%s&signature=%s", getBaseUrl(), query, signature);

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("X-MBX-APIKEY", apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, List.class).getBody();
        } catch (Exception e) {
            log.error("Error fetching account balance.", e);
            return null;
        }
    }

    /**
     * Generates an HMAC-SHA256 signature for the given input data using the specified secret.
     *
     * @param data   the input data to be signed
     * @param secret the secret key used for generating the signature
     * @return the generated HMAC-SHA256 signature as a hexadecimal string
     */
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

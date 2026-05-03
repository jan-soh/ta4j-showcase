package de.jansoh.rsistrategy.service.broker;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration class for managing API-related properties needed to connect to the Binance platform.
 * This class facilitates access to essential API parameters, such as URLs and authentication keys,
 * injected from the application's external configuration.
 * <p>
 * The properties are used for both REST API and WebSocket connections, enabling secure and authenticated
 * communication with the Binance cryptocurrency trading platform.
 * <p>
 * The properties include:
 * - Base URL for REST API access.
 * - WebSocket URL for managing real-time API communication.
 * - API key and secret for authentication purposes.
 * <p>
 * The values of these properties are typically stored in the application's external configuration files
 * and injected into this class using Spring's {@code @Value} annotation.
 * <p>
 * Note: The sensitive data such as API keys and secrets should be handled with care to prevent
 * unauthorized access.
 */
@Component
@Getter
public class ApiConfiguration {

    /**
     * The base URL for accessing Binance's REST API.
     * This constant specifies the endpoint for connecting to Binance's
     * full API services, used for performing secure and authenticated
     * requests to retrieve or manage data related to cryptocurrency
     * trading and account information.
     */
    @Value("${trade.api.url}")
    private String apiUrl;

    /**
     * The WebSocket URL used to connect to the real Binance API.
     * This value is injected from the application configuration and is defined
     * by the property {@code trade.api.websocket.real.url}.
     * <p>
     * It is utilized when the application is configured to work with the real
     * Binance API, as opposed to the test API. This allows the factory class
     * to dynamically determine which URL to use based on the environment.
     */
    @Value("${trade.api.websocket.url}")
    private String websocketApiUrl;

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


}

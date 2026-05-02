package de.jansoh.rsistrategy.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.service.BinanceApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Factory class responsible for creating instances of {@link BinanceOrderEventProvider}.
 * It dynamically determines which WebSocket API URL to use based on the application configuration.
 * The choice between the real API and the test API depends on the value of the {@code binance.use-real-api} property.
 * <p>
 * Dependencies required to construct the {@link BinanceOrderEventProvider} are injected via
 * Spring's dependency injection mechanism.
 * <p>
 * Configuration properties:
 * - {@code binance.use-real-api}: A boolean flag that indicates whether to use the real Binance API.
 * - {@code trade.api.websocket.real.url}: The WebSocket URL for the real Binance API.
 * - {@code trade.api.websocket.test.url}: The WebSocket URL for the Binance test API.
 * <p>
 * This class is a Spring-managed {@code @Service} and makes use of {@code @RequiredArgsConstructor}
 * to manage final field initialization.
 */
@Service
@RequiredArgsConstructor
public class BinanceOrderEventProviderFactory {

    /**
     * Indicates whether the application should connect to the real Binance API or the test API.
     * The value of this property is dynamically injected from the application configuration
     * using the {@code binance.use-real-api} property.
     * <p>
     * If set to {@code true}, the application will use the real API endpoints. If set to {@code false},
     * the application will use the test API endpoints. This property is critical for switching
     * between production and testing environments.
     */
    @Value("${binance.use-real-api}")
    private boolean isRealApi;

    /**
     * The WebSocket URL used to connect to the real Binance API.
     * This value is injected from the application configuration and is defined
     * by the property {@code trade.api.websocket.real.url}.
     * <p>
     * It is utilized when the application is configured to work with the real
     * Binance API, as opposed to the test API. This allows the factory class
     * to dynamically determine which URL to use based on the environment.
     */
    @Value("${trade.api.websocket.real.url}")
    private String realWebsocketApiUrl;

    /**
     * The WebSocket URL used to connect to the Binance test API.
     * This value is dynamically injected from the application configuration using the property
     * {@code trade.api.websocket.test.url}.
     * <p>
     * It is utilized when the application is configured to operate in a testing environment,
     * as determined by the {@code binance.use-real-api} property being set to {@code false}.
     * <p>
     * This allows the application to interact with Binance's test WebSocket endpoints,
     * facilitating testing and development without affecting the production environment.
     */
    @Value("${trade.api.websocket.test.url}")
    private String testWebsocketApiUrl;

    /**
     * Service responsible for interacting with Binance's API.
     * <p>
     * The {@code binanceApiService} is used within the {@link BinanceOrderEventProviderFactory}
     * to enable communication with Binance's REST and WebSocket APIs.
     * <p>
     * This service provides methods and resources necessary for handling API requests,
     * including authentication, order updates, market data retrieval, and other Binance-related operations.
     * <p>
     * It is a core dependency injected into the {@link BinanceOrderEventProviderFactory} via
     * constructor injection to ensure seamless integration and interaction with the Binance API.
     */
    private final BinanceApiService binanceApiService;

    /**
     * A JSON object mapper used for serializing and deserializing objects.
     * <p>
     * The {@code objectMapper} is utilized for processing JSON data, enabling
     * the conversion of Java objects to JSON and vice versa. It plays a critical
     * role in handling complex deserialization and serialization requirements within
     * the factory, particularly when working with WebSocket events or interfacing with
     * the Binance API.
     * <p>
     * As part of the factory's dependencies, this mapper ensures compatibility with the
     * JSON structures expected by Binance's API and the application's internal data
     * representations.
     */
    private final ObjectMapper objectMapper;

    /**
     * A dependency injected into {@link BinanceOrderEventProviderFactory} responsible for mapping
     * raw WebSocket event data representing order updates into structured {@link Order} objects.
     * <p>
     * This mapper plays a key role in deserializing and converting event data received from Binance's
     * WebSocket API into the application's internal order representation. The provided data structure
     * is parsed, validated, and transformed into a domain-specific model, enabling efficient
     * interaction with the processed event information.
     */
    private final OrderUpdateEventMapper orderUpdateEventMapper;

    /**
     * Creates an instance of {@link BinanceOrderEventProvider} by determining the appropriate
     * WebSocket API URL based on the application configuration.
     * <p>
     * The WebSocket API URL is selected dynamically between the real API URL and the test API URL
     * depending on the value of the {@code isRealApi} flag, which is determined at runtime
     * from the application's configuration.
     * <p>
     * Dependencies such as {@code binanceApiService}, {@code objectMapper}, and
     * {@code orderUpdateEventMapper} are injected through the factory class and used
     * to construct the {@link BinanceOrderEventProvider}.
     *
     * @return A fully initialized {@link BinanceOrderEventProvider} instance that can be used
     * to interact with Binance's real or test WebSocket API for processing order events.
     */
    public BinanceOrderEventProvider create() {

        String websocketApiUrl = isRealApi ? realWebsocketApiUrl : testWebsocketApiUrl;

        return new BinanceOrderEventProvider(websocketApiUrl, binanceApiService, objectMapper, orderUpdateEventMapper);
    }
}

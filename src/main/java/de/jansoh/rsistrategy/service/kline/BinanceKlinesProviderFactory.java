package de.jansoh.rsistrategy.service.kline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.service.BinanceApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * A factory class for creating instances of {@link BinanceKlinesProvider}.
 * This class determines the appropriate WebSocket API URL to use based on
 * the application configuration and initializes the {@link BinanceKlinesProvider}
 * with the required dependencies.
 * <p>
 * This service is configured using Spring properties and relies on Spring's
 * dependency injection to provide required components such as
 * {@link BinanceApiService} and {@link ObjectMapper}.
 */
@Service
@RequiredArgsConstructor
public class BinanceKlinesProviderFactory {

    /**
     * Indicates whether the application should use the real Binance API or a test API.
     * This property is injected from the configuration using the key "binance.use-real-api".
     * <p>
     * If set to {@code true}, the application will connect to the real WebSocket API endpoint.
     * If set to {@code false}, the application will connect to the test WebSocket API endpoint.
     */
    @Value("${binance.use-real-api}")
    private boolean isRealApi;

    /**
     * The WebSocket API URL used to connect to the real trading environment.
     * <p>
     * This property is loaded from the application configuration using the key
     * "trade.api.websocket.real.url".
     * <p>
     * It is used when the application is set to connect to the real API,
     * as specified by the "binance.use-real-api" property.
     */
    @Value("${trade.api.websocket.real.url}")
    private String realWebsocketApiUrl;

    /**
     * The WebSocket API URL used to connect to the test trading environment.
     * <p>
     * This property is loaded from the application configuration using the key
     * "trade.api.websocket.test.url".
     * <p>
     * It is used when the application is set to connect to the test API, as determined
     * by the "binance.use-real-api" property being {@code false}.
     */
    @Value("${trade.api.websocket.test.url}")
    private String testWebsocketApiUrl;

    /**
     * An instance of {@link BinanceApiService} that provides functionality for interacting
     * with the Binance API.
     * <p>
     * This service is used for making API requests to Binance and orchestrating
     * communication with the Binance trading and market data endpoints. It is a key
     * dependency in the creation and operation of {@link BinanceKlinesProvider} instances.
     * <p>
     * The instance is injected via dependency injection and is intended to abstract the
     * details of API communication from other components.
     */
    private final BinanceApiService binanceApiService;

    /**
     * An instance of {@link ObjectMapper} used for serializing and deserializing JSON data.
     * <p>
     * This is a key dependency for handling JSON processing within the factory. It is
     * particularly used when creating instances of {@link BinanceKlinesProvider}, which
     * require JSON serialization support for working with Binance API data structures.
     * <p>
     * The object is injected as a final dependency to ensure consistent and reusable JSON
     * handling functionality across the application.
     */
    private final ObjectMapper objectMapper;

    /**
     * Creates an instance of {@link BinanceKlinesProvider} based on the provided trade window.
     * The method determines the appropriate WebSocket API URL (real or test) to use
     * based on the application configuration and initializes the {@link BinanceKlinesProvider}
     * with required dependencies, including the trade window, API service, and JSON object mapper.
     *
     * @param tradeWindow the {@link AssetTradeWindow} instance specifying the trading asset and time window.
     * @return a new instance of {@link BinanceKlinesProvider} configured for the specified trade window.
     */
    public BinanceKlinesProvider create(AssetTradeWindow tradeWindow) {

        String websocketApiUrl = isRealApi ? realWebsocketApiUrl : testWebsocketApiUrl;

        return new BinanceKlinesProvider(tradeWindow, websocketApiUrl, binanceApiService, objectMapper);
    }
}

package de.jansoh.rsistrategy.service.kline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.service.broker.ApiConfiguration;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import lombok.RequiredArgsConstructor;
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
     * A configuration object that encapsulates API-related settings for interacting
     * with the Binance trading platform. This includes URLs for REST and WebSocket
     * endpoints, as well as API credentials required for authentication.
     * <p>
     * The {@code ApiConfiguration} class is typically injected as a dependency into
     * components that require access to these configuration settings, allowing them to
     * interact with Binance APIs in a secure and configurable manner.
     */
    private final ApiConfiguration apiConfiguration;

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

        return new BinanceKlinesProvider(tradeWindow, apiConfiguration.getWebsocketApiUrl(), binanceApiService, objectMapper);
    }
}

package de.jansoh.rsistrategy.service.kline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.BinanceKlineMessage;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.Num;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * The BinanceKlinesProvider class is responsible for connecting to the Binance WebSocket API and
 * consuming kline (candlestick) data streams in real-time. The class also supports initializing historical
 * kline data over the HTTP API and maintains a bar series of candlestick data. It notifies listeners about
 * kline updates upon receiving new data.
 * <p>
 * This class implements the WebSocket.Listener interface to handle events from the WebSocket connection
 * such as text messages, connection opens, errors, and closures.
 */
@Slf4j
@RequiredArgsConstructor
public class BinanceKlinesProvider implements WebSocket.Listener {

    /**
     * Represents the trading window configuration for an asset used within the {@code BinanceKlinesProvider}.
     * <p>
     * The {@code tradeWindow} field defines the asset-specific trading parameters
     * such as the trading symbol, timeframe, and leverage. This field integrates
     * with the functionality of the {@code BinanceKlinesProvider}, enabling
     * it to process and manage market data streams for the specified asset within
     * the configuration described by the {@link AssetTradeWindow} object.
     * <p>
     * Key Characteristics:
     * - Identifies the trading symbol, timeframe, and leverage for handling kline updates.
     * - Serves as the primary configuration for associating trading strategies
     * in the context of Binance market data.
     * - Facilitates asset-specific updates and notifications via associated listener mechanisms.
     * <p>
     * This field is immutable and defined as {@code final}, ensuring that the
     * configuration remains consistent during the lifetime of the {@code BinanceKlinesProvider}.
     */
    private final AssetTradeWindow tradeWindow;

    /**
     * The URL endpoint for connecting to the WebSocket API.
     * <p>
     * This variable holds the WebSocket URL used for connecting
     * to the Binance API to receive real-time kline (candlestick) updates.
     * The URL value depends on whether the application is configured
     * to use the real Binance API or the test API environment, as set
     * in the configuration properties `trade.api.websocket.real.url`
     * and `trade.api.websocket.test.url`.
     * <p>
     * Usage:
     * - It plays a critical role in enabling real-time communication
     * with the Binance server, allowing the application to
     * subscribe to kline channels for specific symbols and timeframes.
     * - The appropriate URL is assigned during the creation of a
     * {@code BinanceKlinesProvider} object.
     * <p>
     * Characteristics:
     * - Immutable and final to ensure the integrity of the WebSocket
     * connection endpoint throughout the lifecycle of the class
     * instance.
     * <p>
     * Related Configuration:
     * The value assigned to this field is determined during the instantiation
     * process in {@code BinanceKlinesProviderFactory}, based on the
     * `isRealApi` flag.
     */
    private final String websocketApiUrl;

    /**
     * Service responsible for interacting with the Binance API.
     * Provides the necessary functionality to fetch data and manage communication
     * with the Binance platform for retrieving market information, trading data, and
     * updates required by the {@code BinanceKlinesProvider}.
     * <p>
     * This service acts as a core component for enabling real-time and historical
     * data operations within the application.
     */
    private final BinanceApiService binanceApiService;

    /**
     * An instance of the {@link ObjectMapper} class used for JSON serialization
     * and deserialization operations within the {@code BinanceKlinesProvider} class.
     * <p>
     * The {@code objectMapper} facilitates the conversion between Java objects
     * and their JSON representations, enabling the processing of data received
     * via the WebSocket API and other JSON-based communication channels.
     * <p>
     * Primary responsibilities:
     * - Parsing JSON messages into Java objects for further processing.
     * - Serializing Java objects into JSON for outbound communication or logging.
     * <p>
     * The configuration and usage of this {@link ObjectMapper} are tailored to
     * support the specific needs of the Binance Klines data handling and
     * other related activities in the class.
     */
    private final ObjectMapper objectMapper;

    /**
     * An {@link HttpClient} instance used for making HTTP requests to external services.
     * <p>
     * This field is responsible for managing and sending asynchronous or synchronous HTTP requests
     * and receiving their responses, typically for interacting with remote APIs such as Binance.
     * <p>
     * Characteristics:
     * - Configurable HTTP client settings like timeouts, authentication, or proxy usage.
     * - Supports HTTP/2 and WebSocket protocols for efficient communication.
     * <p>
     * Use Cases:
     * - Facilitates connections to external services for fetching or streaming data.
     * - Allows handling RESTful API calls, for example, to retrieve market data or perform actions.
     * <p>
     * The client is initialized and utilized by methods implementing specific business logic
     * related to data retrieval and processing for trading operations.
     */
    private HttpClient client;

    /**
     * Represents the name of the WebSocket stream used for receiving
     * real-time Kline (candlestick) updates from the Binance API.
     * <p>
     * This field is dynamically constructed based on the trading symbol
     * and timeframe associated with the current session. The `streamName`
     * plays a critical role in identifying and subscribing to the appropriate
     * WebSocket channel for streaming market data.
     * <p>
     * Characteristics:
     * - Typically follows the format "<symbol>@kline_<timeframe>", where
     * <symbol> is the trading asset pair (e.g., "btcusdt") and <timeframe>
     * is the candlestick interval (e.g., "1m" for one minute).
     * - Used internally for establishing WebSocket connections and managing
     * the lifecycle of real-time data streams.
     * <p>
     * Note:
     * The content of this field must adhere to Binance's WebSocket stream naming
     * conventions in order to function correctly.
     */
    private String streamName;

    /**
     * A thread-safe buffer used for constructing or appending message data
     * within the BinanceKlinesProvider class.
     * <p>
     * This variable is designed to temporarily store incoming message data,
     * typically from a WebSocket stream. Its content is built incrementally
     * and processed when complete message fragments are received.
     * <p>
     * Key Characteristics:
     * - Backed by a StringBuilder to efficiently handle dynamic
     * string concatenations.
     * - Declared as `final` to ensure that the reference cannot be reassigned
     * after initialization.
     * - Primarily used within private methods to assemble and manage data
     * prior to notifying listeners or processing updates.
     * <p>
     * Thread safety should be ensured when accessing or modifying this buffer
     * since it is designed to potentially be called from asynchronous WebSocket
     * events or other multithreaded contexts.
     */
    private final StringBuilder messageBuffer = new StringBuilder();

    /**
     * Maintains a list of listeners for handling KlinesUpdateEvent notifications.
     * <p>
     * The {@code listeners} field holds a collection of {@code KlinesUpdateEventListener}
     * instances. These listeners are notified whenever a KlinesUpdateEvent occurs,
     * allowing components to react to kline updates, such as changes in candle data
     * for a specific trading symbol and timeframe.
     * <p>
     * Features:
     * - Each listener in the list implements the {@code KlinesUpdateEventListener} interface.
     * - Enables decoupling of the event-producing logic from the event-consuming components.
     * - Supports dynamic addition of listeners at runtime.
     * <p>
     * Typical usage scenarios include:
     * - Real-time updates to UI components displaying trading data.
     * - Triggering of actions based on kline updates, such as processing trade signals.
     */
    private final List<KlinesUpdateEventListener> listeners = new ArrayList<>();

    /**
     * Represents a time-series of trading data in the form of a {@link BarSeries}.
     * The field `series` stores the sequence of bars (e.g., candlesticks), which
     * are essential for time-based financial analysis and trading strategies.
     * <p>
     * This variable is updated dynamically as new market data is ingested and
     * processed by the provider, allowing for real-time or historical analyses.
     * <p>
     * Features:
     * - Tracks market bars at specific intervals as defined by the {@link Timeframe}.
     * - Utilized by various methods in the {@code BinanceKlinesProvider} class to
     * process, update, and notify relevant listeners of changes to the series.
     * - Enables integration with the TA4J library for technical analysis.
     * <p>
     * Note:
     * The `series` field is central to performing strategy evaluations,
     * such as calculating indicators, backtesting trading algorithms,
     * and monitoring live market conditions.
     */
    @Getter
    private BarSeries series;

    /**
     * Indicates whether the current update being processed is the first update.
     * <p>
     * This flag is set to `true` by default and can be used to perform specific
     * logic or initialization the first time an update is received. Subsequent
     * updates should reset or manage this flag as necessary.
     * <p>
     * Common use cases include:
     * - Initializing resources only during the first update.
     * - Differentiating between initial setup and regular update processing.
     */
    private boolean firstUpdate = true;

    /**
     * Stores the timestamp of the last update received, represented as the number
     * of milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * <p>
     * This field is used to track and compare the timing of kline data updates
     * received during WebSocket communication or API polling. It ensures that
     * the latest data is processed and helps in determining data freshness.
     * <p>
     * Typical usage scenarios for this field include:
     * - Validating if the received data is newer than previously processed data.
     * - Supporting synchronization of kline updates with external APIs or listeners.
     * - Assisting in determining whether the provider is up-to-date based on recent updates.
     */
    private long lastUpdate = 0;

    /**
     * The URL of the WebSocket API used for real-time data updates.
     * <p>
     * This field holds the base URL required to establish a WebSocket connection
     * with the Binance API. It is determined dynamically based on the application's
     * environment configuration (e.g., real or test modes) and is essential
     * for subscribing to market data streams such as kline updates.
     * <p>
     * Features:
     * - Determines the WebSocket endpoint for connection.
     * - Used internally to facilitate real-time communication with Binance's server.
     * - Supports dynamic selection between real and test API endpoints.
     * <p>
     * Note:
     * The value of this field is typically injected into the provider during its
     * instantiation and remains constant for the lifetime of the object.
     */
    private String wsUrl;

    /**
     * Initiates or restarts the WebSocket client for subscribing to Binance Klines updates.
     *
     * <ul>
     * - If an existing WebSocket client connection is active, it is closed.
     * - Resets the state for tracking Kline updates.
     * - Initializes the necessary configurations and sets up the WebSocket URL specific to the target trade window.
     * - Builds and asynchronously starts the WebSocket client for handling real-time market data streams.
     * <p>
     * Logs the start of the WebSocket client for the associated market stream.
     */
    public void start() {

        if (null != client) {
            client.close();
        }

        lastUpdate = Long.MAX_VALUE;
        firstUpdate = true;

        init();


        wsUrl = websocketApiUrl + "/market/ws/" + streamName;

        client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), this);

        log.info("----- WEB_SOCKET_KLINES ----- HTTP client for stream {} started", wsUrl);
    }

    private void init() {

        streamName = String.format("%s@kline_%s", tradeWindow.getSymbol().toLowerCase(), tradeWindow.getTimeframe().getShortcut());
        series = new BaseBarSeriesBuilder().withName(tradeWindow.getSymbol()).build();

        List<Object[]> klines = binanceApiService.getKlines(tradeWindow.getSymbol(), tradeWindow.getTimeframe().getShortcut(), 1500);
        for (Object[] k : klines) {
            addBar(k);
        }

        log.info("----- WEB_SOCKET_KLINES ----- klines for symbol {} at timeframe {} initialized with {} bars.", tradeWindow.getSymbol(), tradeWindow.getTimeframe().getShortcut(), klines.size());
    }

    private void addBar(Object[] k) {
        // Binance Kline format:
        // [0] Open time, [1] Open, [2] High, [3] Low, [4] Close, [5] Volume, [6] Close time...
        Instant openTime = Instant.ofEpochMilli(Long.parseLong(k[0].toString()));
        Instant endTime = Instant.ofEpochMilli(Long.parseLong(k[6].toString()));
        Num open = series.numFactory().numOf(k[1].toString());
        Num high = series.numFactory().numOf(k[2].toString());
        Num low = series.numFactory().numOf(k[3].toString());
        Num close = series.numFactory().numOf(k[4].toString());
        Num volume = series.numFactory().numOf(k[5].toString());

        BaseBar bar = new BaseBar(null, openTime, endTime, open, high, low, close, volume, series.numFactory().numOf(0), 0);
        series.addBar(bar);
    }

    /**
     * Handles the event when the WebSocket connection is successfully opened.
     * This method is called automatically upon the establishment of
     * a WebSocket connection.
     *
     * @param webSocket the {@link WebSocket} instance representing the newly opened connection.
     */
    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        log.info("----- WEB_SOCKET_KLINES ----- klines listener connected to {}.", wsUrl);
    }

    /**
     * Handles the event when the WebSocket connection is closed.
     * This method is invoked when the WebSocket connection is terminated,
     * either due to normal closure or an error.
     * <p>
     * Logs the details of the closure, including the WebSocket stream name,
     * status code, and reason for closure. After logging, it initiates a
     * reconnection attempt by calling the {@code start()} method to restart
     * the WebSocket client for the market stream.
     *
     * @param webSocket  the {@link WebSocket} instance representing the closed connection
     * @param statusCode the status code indicating the reason the connection was closed
     * @param reason     a textual description of the closure reason provided by the server
     * @return a {@link CompletionStage} representing the asynchronous completion
     * of any operations performed during the closure handling
     */
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {

        log.info("----- WEB_SOCKET_KLINES ----- stream closed: {}, code {}, reason {}.", streamName, statusCode, reason);
        start();
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    /**
     * Handles errors encountered during WebSocket communication.
     * This method is invoked automatically when an error occurs within the WebSocket connection.
     * Logs the error details, including the associated WebSocket stream name and the error itself.
     *
     * @param webSocket the {@link WebSocket} instance where the error occurred
     * @param error     the {@link Throwable} representing the encountered error
     */
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("----- WEB_SOCKET_KLINES ----- error for stream {}.", streamName, error);
    }

    /**
     * Processes incoming text messages received from a WebSocket connection.
     * The method appends the received data to a message buffer and, if the
     * message is complete (indicated by the `last` parameter), triggers
     * further processing by calling the {@code onKlineUpdate} method.
     *
     * @param webSocket the WebSocket instance from which the message originated
     * @param data      the partial or full text message received over the WebSocket
     * @param last      a flag indicating whether this is the final part of the message
     * @return a {@link CompletionStage} representing the asynchronous completion
     * of the message handling
     */
    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

        messageBuffer.append(data);

        if (last) {

            String fullMessage = messageBuffer.toString();
            onKlineUpdate(fullMessage);

            messageBuffer.setLength(0);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    private void onKlineUpdate(String message) {
        try {
            BinanceKlineMessage klineMessage = objectMapper.readValue(message, BinanceKlineMessage.class);
            BinanceKlineMessage.KlineData k = klineMessage.getKline();

            if (k != null && k.getIsClosed()) {

                long lastTimestamp = series.getLastBar().getEndTime().toEpochMilli();
                log.debug("----- WEB_SOCKET_KLINES ----- last kline end time: {}, current end time: {}", lastTimestamp, k.getCloseTime());
                if (firstUpdate && k.getCloseTime() <= lastTimestamp) {
                    updateAndNotifyListeners(k, true);
                    firstUpdate = false;
                }
                if (k.getCloseTime() > lastTimestamp) {
                    updateAndNotifyListeners(k, false);
                }
            }
            lastUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Error parsing kline update message: {}", message, e);
        }
    }

    private void updateAndNotifyListeners(BinanceKlineMessage.KlineData k, boolean replaceExisting) {
        addBarFromWebSocket(k, replaceExisting);
        KlinesUpdateEvent klinesUpdateEvent = KlinesUpdateEventImpl.builder()
                .symbol(tradeWindow.getSymbol())
                .timeframe(tradeWindow.getTimeframe())
                .barSeries(series)
                .build();

        notifyKlinesUpdateListenersUpdate(klinesUpdateEvent);
    }

    private void addBarFromWebSocket(BinanceKlineMessage.KlineData k, boolean replaceExisting) {

        Instant openTime = Instant.ofEpochMilli(k.getStartTime());
        Instant endTime = Instant.ofEpochMilli(k.getCloseTime());
        Num open = series.numFactory().numOf(k.getOpenPrice());
        Num high = series.numFactory().numOf(k.getHighPrice());
        Num low = series.numFactory().numOf(k.getLowPrice());
        Num close = series.numFactory().numOf(k.getClosePrice());
        Num volume = series.numFactory().numOf(k.getVolume());

        BaseBar bar = new BaseBar(null, openTime, endTime, open, high, low, close, volume, series.numFactory().numOf(0), 0);
        series.addBar(bar, replaceExisting);

        log.info("----- WEB_SOCKET_KLINES ----- new kline for symbol {} at timeframe {} at {}.", tradeWindow.getSymbol(), tradeWindow.getTimeframe().getShortcut(), endTime);
    }

    /**
     * Registers a new listener that will be notified of Kline update events.
     *
     * @param listener the {@link KlinesUpdateEventListener} instance to add.
     *                 This listener's {@code onKlinesUpdate} method will be invoked
     *                 whenever a new Kline update event is received.
     */
    public void addKlineUpdateEventListener(KlinesUpdateEventListener listener) {
        listeners.add(listener);
    }

    private void notifyKlinesUpdateListenersUpdate(KlinesUpdateEvent event) {
        listeners.forEach(listener -> listener.onKlinesUpdate(event));
    }

    /**
     * Determines whether the current data associated with the Binance Klines provider
     * is up-to-date based on the last update time and the duration of the trade window's timeframe.
     *
     * @return {@code true} if the data is considered up-to-date, meaning the difference
     * between the current time and the last update time (adjusted by the timeframe)
     * is negative. {@code false} otherwise.
     */
    public boolean isUpToDate() {
        long diff = System.currentTimeMillis() - lastUpdate + 1000 - tradeWindow.getTimeframe().getMinutes() * 60000L;
        return diff < 0;
    }
}

package de.jansoh.rsistrategy.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.Order;
import de.jansoh.rsistrategy.service.broker.ApiConfiguration;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * The BinanceOrderEventProvider class establishes and manages a WebSocket connection
 * to the Binance API in order to receive real-time order updates for a user. It processes
 * and dispatches order update events to registered listeners.
 * <p>
 * This class implements the WebSocket.Listener interface to handle WebSocket events such
 * as open, close, error, and incoming messages. It is responsible for the following:
 * <p>
 * 1. Establishing a WebSocket connection with the Binance API using a secure listen key
 * provided by the Binance API service.
 * <p>
 * 2. Managing the lifecycle of the WebSocket connection, including initialization,
 * reconnections, and keep-alive operations through periodic tasks using the @Scheduled
 * annotation.
 * <p>
 * 3. Parsing and mapping incoming WebSocket messages that contain order trade updates into
 * domain objects.
 * <p>
 * 4. Notifying registered event listeners of updated order information via callback
 * mechanisms.
 * <p>
 * Core operations of the class:
 * <p>
 * - The {@code start()} method establishes the WebSocket connection and initializes the
 * required infrastructure.
 * <p>
 * - The {@code keepAlive()} method periodically keeps the Binance listen key alive to
 * prevent disconnection.
 * <p>
 * - The {@code restart()} method handles the reconnection logic, ensuring that the WebSocket
 * connection is re-established if needed.
 * <p>
 * - The {@code onOrderUpdate(String message)} method processes incoming WebSocket messages,
 * extracts relevant data, and notifies listeners of order updates.
 * <p>
 * - The {@code addOrderUpdateEventListener(OrderUpdateEventListener listener)} method allows
 * registration of listeners that will be notified of order update events.
 * <p>
 * The class maintains synchronization to ensure reliability in managing the WebSocket
 * lifecycle and processing messages.
 * <p>
 * Dependencies:
 * - Requires a BinanceApiService to interact with the Binance REST API for listen key
 * management.
 * - Utilizes an ObjectMapper for JSON deserialization.
 * - Relies on an OrderUpdateEventMapper to transform raw API events into domain objects.
 * - The logging framework is used to provide operational insights and error reporting.
 * <p>
 * Key annotations:
 * - @Slf4j: Provides a logger for logging purposes.
 * - @RequiredArgsConstructor: Automatically generates constructor for final fields.
 * - @Scheduled: Used to schedule periodic tasks, such as keeping the connection alive
 * and restarting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceOrderEventProvider implements WebSocket.Listener {

    /**
     * Represents the configuration settings used to interact with an API.
     * This variable holds essential details such as base URLs, authentication
     * keys, timeouts, and other parameters required for establishing and managing
     * API communication.
     */
    private final ApiConfiguration apiConfiguration;

    /**
     * A service used for interacting with the Binance API.
     * This service provides functionality to communicate with Binance's various API endpoints
     * needed for handling order events and updates in the application.
     * It acts as a core dependency for the BinanceOrderEventProvider class.
     */
    private final BinanceApiService binanceApiService;

    /**
     * A Jackson {@link ObjectMapper} instance used for JSON serialization and deserialization.
     * This object facilitates the conversion of JSON data to Java objects and vice versa,
     * and plays a crucial role in processing data received or sent via the WebSocket connection
     * for order update events.
     * <p>
     * The {@code objectMapper} is configured to handle complex JSON objects and is capable of
     * efficiently mapping data structures as part of the order update processing workflow.
     */
    private final ObjectMapper objectMapper;

    /**
     * A mapper responsible for converting raw event data into an {@link Order} object.
     * This mapper facilitates the deserialization and transformation of data received
     * from order update events into a structured format that can be further processed.
     * <p>
     * This field is used to interpret raw event payloads into domain-specific objects
     * and is integral to handling Binance order updates within the application.
     */
    private final OrderUpdateEventMapper orderUpdateEventMapper;

    /**
     * An HTTP client used for making network requests within the BinanceOrderEventProvider.
     * The client facilitates communication with the Binance WebSocket API and other
     * associated services by handling HTTP-based requests and responses.
     */
    private HttpClient client;

    /**
     * Represents the name of the stream used for subscribing to Binance WebSocket events.
     * This variable serves as an identifier for the channel of real-time updates related
     * to order events on the Binance platform.
     */
    private String streamName;

    /**
     * A buffer used to accumulate messages received via WebSocket communication.
     * This buffer is primarily utilized to hold partial or complete message data
     * before processing or for temporary message storage during message streaming operations.
     * It allows efficient handling and manipulation of the aggregated data
     * before converting it into a suitable format for further processing.
     */
    private final StringBuilder messageBuffer = new StringBuilder();

    /**
     * A collection of listeners that are notified when an order update event occurs.
     * <p>
     * Each listener in this list must implement the {@link OrderUpdateEventListener} interface, which
     * defines the method {@code onOrderUpdate(OrderUpdateEvent event)}. The listeners are typically
     * registered using the {@code addOrderUpdateEventListener(OrderUpdateEventListener listener)}
     * method provided by the containing class.
     * <p>
     * When an order update event is detected, the containing class invokes the
     * {@code notifyOrderUpdateListeners(OrderUpdateEvent event)} method, which iterates over
     * this list and calls the {@code onOrderUpdate} method of each registered listener.
     * <p>
     * The listeners are used to process updates such as order status changes, filled quantities,
     * or price updates provided through the {@link OrderUpdateEvent}.
     */
    private final List<OrderUpdateEventListener> listeners = new ArrayList<>();
    private String wsUrl;


    /**
     * Indicates whether the Binance order event provider is currently active and operational.
     * This flag is used to track the availability of the WebSocket connection
     * and the processing of Binance order update events.
     */
    @Getter
    private boolean available;

    /**
     * Indicates whether the automatic restart of the WebSocket connection
     * in the BinanceOrderEventProvider should be prevented.
     * <p>
     * When set to {@code true}, the WebSocket connection will not be automatically
     * restarted even if it is closed or encounters an error, allowing for manual
     * control over the reconnection behavior.
     * <p>
     * This flag is typically used in scenarios where reconnection logic
     * needs to be overridden or disabled for customization or debugging purposes.
     */
    @Setter
    private boolean preventRestart;

    private CompletableFuture<WebSocket> webSocketFuture;

    /**
     * Initializes and starts the WebSocket connection for receiving order update events.
     * <p>
     * This method performs the following steps:
     * 1. Marks the provider as unavailable by setting the `available` flag to false.
     * 2. Prevents immediate restarting during the initialization process by setting `preventRestart` to true.
     * 3. Closes any existing WebSocket client connection, if present.
     * 4. Calls the `init` method to initialize resources, such as retrieving the stream name from the Binance API service.
     * 5. Constructs a new WebSocket URL using the `websocketApiUrl` and the `streamName`.
     * 6. Initializes a new `HttpClient` and starts an asynchronous WebSocket connection using the constructed URL.
     * 7. Resets the `preventRestart` flag, allowing future restart operations.
     * <p>
     * Preconditions:
     * - The `websocketApiUrl` must be correctly set and represent the base URL for the WebSocket API.
     * - The `binanceApiService` dependency must be properly configured to provide the user data stream key.
     * <p>
     * Postconditions:
     * - A new WebSocket connection is established for listening to order events.
     * - The `available` flag will remain false until the WebSocket connection is successfully opened.
     * <p>
     * Side Effects:
     * - Sets the `wsUrl` field to the WebSocket URL.
     * - Updates the `client` field with a new instance of `HttpClient`.
     * - Closes any previously open WebSocket connection, if applicable.
     * <p>
     * Thread Safety:
     * - Not explicitly thread-safe; external synchronization might be required if accessed concurrently.
     * <p>
     * Potential Exceptions:
     * - If the stream name cannot be retrieved through the `init` method, the WebSocket connection will not be established.
     * <p>
     * Usage Context:
     * - Typically invoked during application start-up or to re-establish the WebSocket connection.
     * - Can also be called within periodic maintenance tasks, such as `keepAlive` or `restart`.
     */
    public void start() {

        available = false;
        preventRestart = true;

        if (null == client) {
            client = HttpClient.newHttpClient();
        }

        init();
        preventRestart = false;
    }

    private void init() {

        // first close an existing WebSocket connection
        if (null != webSocketFuture) {
            webSocketFuture.thenAccept(WebSocket::abort);
        }

        streamName = binanceApiService.startUserDataStream();
        if (streamName == null) {
            log.error("----- WEB_SOCKET_ORDERS ----- failed to get Binance Listen Key. User data WebSocket connection aborted.");
            return;
        }

        wsUrl = apiConfiguration.getWebsocketApiUrl() + "/private/ws/" + streamName;

        webSocketFuture = client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), this);
    }

    /**
     * Periodically ensures the user data stream remains active by sending a keep-alive signal.
     * <p>
     * The method checks whether the `streamName` field is initialized. If true, it invokes the
     * `keepAliveUserDataStream` method from `binanceApiService` to maintain the user data stream.
     * If `streamName` is not initialized, it calls the `start` method to re-establish the WebSocket connection.
     * <p>
     * Scheduling:
     * - This method is scheduled to execute at fixed intervals of 30 minutes using the `@Scheduled` annotation.
     * <p>
     * Preconditions:
     * - The `binanceApiService` dependency must be correctly initialized to interact with the Binance API.
     * - The `streamName` must be non-null to avoid reinitializing the WebSocket connection unnecessarily.
     * <p>
     * Postconditions:
     * - If `streamName` is non-null, the user data stream is kept alive.
     * - If `streamName` is null, the `start` method is invoked to reinitialize the connection.
     * <p>
     * Usage Context:
     * - Typically used as a maintenance operation to avoid user data stream expiration.
     * <p>
     * Potential Exceptions:
     * - Any errors encountered during `binanceApiService.keepAliveUserDataStream` or the `start` method
     * will be logged by their respective implementations.
     * <p>
     * Thread Safety:
     * - Not explicitly thread-safe; concurrent executions may require synchronization if other methods modify `streamName`.
     */
    @Scheduled(initialDelay = 30, fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void keepAlive() {
        log.info("----- WEB_SOCKET_ORDERS ----- going to keep alive user stream.");
        if (streamName != null) {
            binanceApiService.keepAliveUserDataStream();
        } else {
            start();
        }
    }

    /**
     * Attempts to restart the WebSocket connection and resets the necessary state
     * to ensure continued operation of the order event provider.
     * <p>
     * This method is periodically triggered using a scheduled task and incorporates
     * a delay mechanism to avoid immediate retries if the restart is prevented
     * due to ongoing initialization or other constraints.
     * <p>
     * Behavior:
     * - The method starts by retrying for a fixed number of attempts (controlled
     * by the `tries` variable) to ensure that restarts do not interfere with
     * critical tasks controlled by the `preventRestart` flag.
     * - During each iteration, it delays execution for 1 second before retrying
     * if the `preventRestart` flag is true.
     * - If the maximum number of retries is reached and the restart is still
     * prevented, an error message is logged, and the restart proceeds regardless.
     * - Finally, the `start` method is invoked to reinitialize the WebSocket connection.
     * <p>
     * Preconditions:
     * - The `preventRestart` flag must accurately reflect whether the restart should
     * be temporarily deferred.
     * <p>
     * Postconditions:
     * - If allowed, the WebSocket connection is restarted and reinitialized.
     * - Logs an error if the maximum retry attempts are exhausted while waiting for
     * the restart flag to clear.
     * <p>
     * Side Effects:
     * - Causes the WebSocket connection to be closed and reopened by invoking the `start` method.
     * - Delays method execution during retries.
     * - Produces log output if restart is forcibly executed after exceeding retries.
     * <p>
     * Scheduling:
     * - This method is executed at fixed intervals of 12 hours, as specified by
     * the `@Scheduled` annotation with `TimeUnit.HOURS`.
     * <p>
     * Thread Safety:
     * - Not explicitly thread-safe. Concurrent modifications to the `preventRestart`
     * flag or related fields may require synchronization.
     * <p>
     * Potential Exceptions:
     * - May throw a `RuntimeException` if interrupted during the `Thread.sleep` call.
     * - Any exceptions from the `start` method are handled within its implementation.
     * <p>
     * Usage Context:
     * - Used to maintain the availability and operability of the order event provider
     * by periodically restarting the connection to handle transient issues or stale state.
     */
    //@Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
    // this is not required maybe
    public void restart() {

        log.info("----- WEB_SOCKET_ORDERS ----- order event provider is going to be restarted.");

        int tries = 10;
        while (tries-- > 0 && preventRestart) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (0 == tries) {
            log.error("----- WEB_SOCKET_ORDERS ----- order event provider was prevented from restarting for too long. The restart will happen now.");
        }
        start();
    }

    /**
     * Handles the opening of a WebSocket connection.
     * <p>
     * This method is invoked when the WebSocket connection is successfully established. It logs
     * a message indicating that the connection has been established and updates relevant
     * internal state to mark the provider as available and ready to handle messages.
     *
     * @param webSocket the WebSocket instance representing the newly established connection
     */
    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        log.info("----- WEB_SOCKET_ORDERS ----- orders listener connected to {}.", wsUrl);
        available = true;
        preventRestart = false;
    }

    /**
     * Invoked when the WebSocket connection is closed.
     * <p>
     * This method handles the closure of the WebSocket connection by performing the following actions:
     * - Sets the `available` flag to false, indicating that the provider is no longer available for processing.
     * - Logs the closure event details, including the associated stream name, status code, and reason.
     * - Triggers the `start` method to attempt reconnection or reinitialization of the WebSocket connection.
     * <p>
     * The default implementation of the `onClose` method in {@link WebSocket.Listener} is also invoked.
     *
     * @param webSocket  the WebSocket instance that was closed
     * @param statusCode the status code indicating the reason for the closure
     * @param reason     a textual reason for why the WebSocket connection was closed
     * @return a {@link CompletionStage} instance representing the asynchronous result of the operation
     */
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {

        available = false;
        log.info("----- WEB_SOCKET_ORDERS ----- stream closed: {}, code {}, reason {}.", streamName, statusCode, reason);
        start();
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    /**
     * Handles the error event for the WebSocket connection. This method will capture
     * the details of the error, log the incident, and update the state of the connection.
     *
     * @param webSocket the WebSocket instance where the error occurred
     * @param error     the Throwable instance representing the error that occurred
     */
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("----- WEB_SOCKET_KLINES ----- error for stream {}.", streamName, error);
        available = false;
        preventRestart = false;
    }

    /**
     * Handles text messages received via a WebSocket connection. The method
     * appends the received text data to an internal buffer and processes the
     * complete message when the last fragment is received.
     *
     * @param webSocket the WebSocket instance from which the message is received
     * @param data      the text data received from the WebSocket
     * @param last      a boolean indicating whether the current message fragment is the last part of the message
     * @return a CompletionStage representing the asynchronous processing of the text event
     */
    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

        messageBuffer.append(data);

        if (last) {

            String fullMessage = messageBuffer.toString();
            onOrderUpdate(fullMessage);

            messageBuffer.setLength(0);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    private void onOrderUpdate(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = event.get("e") != null ? event.get("e").toString() : null;

            log.info("----- WEB_SOCKET_ORDERS ----- order update event received:\n {}", message);

            if ("ORDER_TRADE_UPDATE".equals(eventType)) {

                Order order = orderUpdateEventMapper.map(event);
                OrderUpdateEvent orderUpdateEvent = OrderUpdateEventImpl.builder()
                        .order(order)
                        .build();

                notifyOrderUpdateListeners(orderUpdateEvent);
            }
        } catch (Exception e) {
            log.error("----- WEB_SOCKET_ORDERS ----- error processing order update message:\n {}", message, e);
        }
    }

    /**
     * Registers a new listener to receive order update events. The listener will be notified
     * whenever an order is updated within the system.
     *
     * @param listener the {@code OrderUpdateEventListener} instance to be added to the list of listeners
     */
    public void addOrderUpdateEventListener(OrderUpdateEventListener listener) {
        listeners.add(listener);
    }

    private void notifyOrderUpdateListeners(OrderUpdateEvent event) {
        listeners.forEach(listener -> listener.onOrderUpdate(event));
    }
}

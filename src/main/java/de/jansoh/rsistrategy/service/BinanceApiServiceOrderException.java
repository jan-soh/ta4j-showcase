package de.jansoh.rsistrategy.service;

/**
 * Exception class representing errors that occur while interacting with the Binance API
 * for order-related operations.
 * <p>
 * This exception is a subclass of {@link RuntimeException} and is intended to be used
 * when there is an issue in order processing, such as invalid parameters, API errors,
 * or unexpected behaviors during communication with Binance's services.
 * <p>
 * The exception provides two constructors:
 * - One for specifying an error message.
 * - Another for specifying both an error message and a root cause throwable.
 */
public class BinanceApiServiceOrderException extends RuntimeException {

    /**
     * Constructs a new {@code BinanceApiServiceOrderException} with the specified detail message
     * and cause. This exception is typically used to indicate issues related to order processing
     * while interacting with the Binance API, such as invalid parameters, API errors, or communication failures.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying cause of the exception, which may be {@code null}
     */
    public BinanceApiServiceOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}

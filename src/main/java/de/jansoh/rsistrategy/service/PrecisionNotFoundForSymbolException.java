package de.jansoh.rsistrategy.service;

/**
 * Exception thrown to signal that the precision for a specific symbol
 * could not be found in the system.
 * <p>
 * This exception is typically used in the context of operations where
 * a symbol's precision needs to be retrieved from a data source, such
 * as a mapping or a service, and the requested symbol does not exist
 * or is not configured.
 * <p>
 * Designed to be used in services like {@code PrecisionService} to indicate
 * an error when attempting to retrieve precision data for a symbol that is
 * not present in the precision map.
 */
public class PrecisionNotFoundForSymbolException extends RuntimeException {

    /**
     * Constructs a new {@code PrecisionNotFoundForSymbolException} with the specified detail message.
     * This exception is thrown when the precision for a specific trading symbol is not found
     * in the precision map or data source.
     *
     * @param message the detail message providing additional context about the exception;
     *                cannot be null or empty.
     */
    public PrecisionNotFoundForSymbolException(String message) {
        super(message);
    }
}

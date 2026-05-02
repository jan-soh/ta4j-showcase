package de.jansoh.rsistrategy.service.strategy;

/**
 * Exception thrown to indicate that an illegal state has occurred within the Strategy Service.
 * <p>
 * This exception is a subclass of RuntimeException, providing an unchecked exception
 * to signal that a specific invalid state has been encountered. It can be used to handle
 * errors related to improper usage or unexpected conditions in the context of the Strategy Service.
 */
public class StrategyServiceIllegalStateException extends RuntimeException {

    /**
     * Constructs a new StrategyServiceIllegalStateException with the specified detail message.
     *
     * @param message the detail message that provides more information about the exception.
     */
    public StrategyServiceIllegalStateException(String message) {
        super(message);
    }
}

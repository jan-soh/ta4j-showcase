package de.jansoh.rsistrategy.service.position;

/**
 * Indicates an exception that occurs when there is an issue with registering
 * an open position in the system.
 * <p>
 * This exception is a runtime exception and should be used to signify
 * situations where the registration of an open position fails due to
 * business rules or operational constraints.
 */
public class OpenPositionRegistrationException extends RuntimeException {

    /**
     * Constructs a new OpenPositionRegistrationException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception.
     *                It provides additional context about why the open position
     *                registration operation has failed.
     */
    public OpenPositionRegistrationException(String message) {
        super(message);
    }
}

package de.jansoh.rsistrategy.service;

/**
 * Interface for broadcasting messages to recipients or connected systems.
 * This interface defines a contract for broadcast operations, which allows
 * sending message data in the form of text.
 */
public interface MessageService {

    /**
     * Broadcasts a text message to all recipients or systems that are connected
     * and configured to receive messages through this service.
     *
     * @param text the message content to be broadcasted; must not be null.
     */
    void broadcast(String text);
}

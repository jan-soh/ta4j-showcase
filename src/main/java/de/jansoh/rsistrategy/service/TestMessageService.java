package de.jansoh.rsistrategy.service;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * A service implementation for broadcasting messages, designed primarily for
 * testing and local development environments. This service outputs the broadcasted
 * messages to the console instead of sending them to external systems or recipients.
 * <p>
 * This class is marked as the primary implementation of the {@code MessageService} interface
 * and is activated when the application is running under the profiles "default",
 * "local", or "dev".
 * <p>
 * The purpose of this implementation is to facilitate testing and debugging by
 * providing a lightweight and self-contained way to handle message broadcasting.
 * <p>
 * Annotations:
 * - {@code @Component}: Marks this class as a Spring-managed bean.
 * - {@code @Primary}: Designates this as the default implementation of
 * {@code MessageService} when multiple implementations are available.
 * - {@code @Profile({"default", "local", "dev"})}: Limits the activation of this
 * service to specific runtime profiles.
 */
@Component
@Primary
@Profile({"default", "local", "dev"})
public class TestMessageService implements MessageService {

    /**
     * Broadcasts a text message to all recipients or systems by printing it to the console.
     * This implementation is designed for testing and local development purposes, where
     * messages are output to the console instead of being sent to external systems.
     *
     * @param text the message content to be broadcasted; must not be null.
     */
    @Override
    public void broadcast(String text) {
        System.out.println("MESSAGE SERVICE: " + text);
    }
}

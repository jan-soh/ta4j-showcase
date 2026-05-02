package de.jansoh.rsistrategy;

import de.jansoh.rsistrategy.service.strategy.StrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The main entry point for the TA4J application.
 * This application utilizes the Spring Boot framework and includes scheduled tasks.
 * It initializes and starts the configured trading strategy service upon application readiness.
 * <p>
 * Annotations:
 * - {@code @SpringBootApplication}: Marks the class as a Spring Boot application.
 * - {@code @EnableScheduling}: Enables scheduling support within the application.
 * <p>
 * Components:
 * - {@code StrategyService}: A service responsible for managing and executing trading strategies.
 * It is automatically injected by Spring's dependency injection mechanism.
 * <p>
 * Methods:
 * - {@code main(String[] args)}: The main method used to launch the application.
 * - {@code initialize()}: A method that triggers the start of the strategy service when the
 * application is fully initialized and ready.
 * <p>
 * Events:
 * - {@code ApplicationReadyEvent}: Used to detect when the application is ready to execute
 * initialization logic.
 */
@SpringBootApplication
@EnableScheduling
public class TA4JApp {

    /**
     * The {@code strategyService} variable represents a Spring-managed bean of type {@code StrategyService}.
     * It is responsible for managing and executing trading strategies within the application.
     * <p>
     * Annotations:
     * - {@code @Autowired}: Indicates that the {@code StrategyService} implementation is automatically
     * injected by Spring's dependency injection framework, ensuring the application has access
     * to the necessary methods for strategy management.
     * <p>
     * Usage:
     * The {@code strategyService} is initialized and used within the application context to perform
     * operations related to trading strategies, such as starting or managing them upon application readiness.
     */
    @Autowired
    private StrategyService strategyService;

    /**
     * The main entry point for the TA4J application.
     * This method starts the application using the Spring Boot framework.
     *
     * @param args an array of command-line arguments passed to the application
     */
    static void main(String[] args) {
        SpringApplication.run(TA4JApp.class, args);
    }

    /**
     * Initializes the trading strategy service when the application is fully started and ready.
     * <p>
     * This method is automatically triggered by the Spring framework upon receiving the
     * {@code ApplicationReadyEvent}. It delegates the initialization process to the
     * {@code strategyService} by invoking its {@code start()} method, which sets up and begins
     * monitoring trade windows for the configured strategies. If the service is already running,
     * the {@code strategyService.start()} method ensures no redundant initialization takes place.
     * <p>
     * Event:
     * - {@code ApplicationReadyEvent}: Indicates that the application context has been completely
     * initialized and the application is ready for execution.
     * <p>
     * Dependencies:
     * - {@code strategyService}: The service responsible for managing and executing trading strategies.
     * It must be configured and injected into the application context before this method is executed.
     * <p>
     * Side Effects:
     * - Calls the {@code start()} method of the strategy service, which triggers the initialization
     * and startup of the trading strategies.
     * <p>
     * Annotations:
     * - {@code @EventListener}: Marks this method as an event listener for the {@code ApplicationReadyEvent}.
     * Ensures the method is executed automatically when the application context is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    private void initialize() {
        strategyService.start();
    }
}

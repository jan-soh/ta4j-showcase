package de.jansoh.rsistrategy;

import de.jansoh.rsistrategy.service.strategy.StrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TS4JApp {

    @Autowired
    private StrategyService strategyService;

    static void main(String[] args) {
        SpringApplication.run(TS4JApp.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void initialize() {
        strategyService.start();
    }
}

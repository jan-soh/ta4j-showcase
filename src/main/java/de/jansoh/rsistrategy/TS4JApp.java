package de.jansoh.rsistrategy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TS4JApp {

    public static void main(String[] args) {
        SpringApplication.run(TS4JApp.class, args);
    }

}

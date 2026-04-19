package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class StrategyConfigurationLoader {

    private static final Logger log = LoggerFactory.getLogger(StrategyConfigurationLoader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public static <T> T loadConfiguration(Class<T> clazz) {
        StrategyProperties annotation = clazz.getAnnotation(StrategyProperties.class);
        if (annotation == null) {
            log.warn("Class {} is not annotated with @StrategyProperties. Using default configuration.", clazz.getSimpleName());
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate default configuration for " + clazz.getSimpleName(), e);
            }
        }

        String profile = annotation.value();
        String fileName = String.format("/strategy/configuration/%s-%s.json", clazz.getSimpleName(), profile);

        try (InputStream inputStream = StrategyConfigurationLoader.class.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                log.error("Configuration file {} not found. Falling back to default configuration.", fileName);
                return clazz.getDeclaredConstructor().newInstance();
            }
            return objectMapper.readValue(inputStream, clazz);
        } catch (Exception e) {
            log.error("Error loading configuration from {}. Falling back to default configuration.", fileName, e);
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to instantiate default configuration for " + clazz.getSimpleName(), ex);
            }
        }
    }

    public static void populate(Object instance) {
        Class<?> clazz = instance.getClass();
        StrategyProperties annotation = clazz.getAnnotation(StrategyProperties.class);
        if (annotation == null) return;

        String profile = annotation.value();
        String fileName = String.format("/strategy/configuration/%s-%s.json", clazz.getSimpleName(), profile);

        try (InputStream inputStream = StrategyConfigurationLoader.class.getResourceAsStream(fileName)) {
            if (inputStream != null) {
                objectMapper.readerForUpdating(instance).readValue(inputStream);
                log.info("Successfully populated {} from {}", clazz.getSimpleName(), fileName);
            }
        } catch (Exception e) {
            log.error("Failed to populate {} from {}", clazz.getSimpleName(), fileName, e);
        }
    }
}

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
        StrategyProperties annotation = findAnnotation(clazz);
        if (annotation == null) {
            log.warn("Class {} is not annotated with @StrategyProperties. Using default configuration.", clazz.getSimpleName());
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate default configuration for " + clazz.getSimpleName(), e);
            }
        }

        String profile = annotation.value();

        try (InputStream inputStream = findResource(clazz, profile)) {
            if (inputStream == null) {
                log.error("Configuration file for class {} with profile {} not found. Falling back to default configuration.", clazz.getSimpleName(), profile);
                return clazz.getDeclaredConstructor().newInstance();
            }
            return objectMapper.readValue(inputStream, clazz);
        } catch (Exception e) {
            log.error("Error loading configuration for {} with profile {}. Falling back to default configuration.", clazz.getSimpleName(), profile, e);
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to instantiate default configuration for " + clazz.getSimpleName(), ex);
            }
        }
    }

    public static void populate(Object instance) {
        Class<?> clazz = instance.getClass();
        StrategyProperties annotation = findAnnotation(clazz);
        if (annotation == null) return;

        String profile = annotation.value();

        try (InputStream inputStream = findResource(clazz, profile)) {
            if (inputStream != null) {
                objectMapper.readerForUpdating(instance).readValue(inputStream);
                log.info("Successfully populated {} from profile {}", clazz.getSimpleName(), profile);
            }
        } catch (Exception e) {
            log.error("Failed to populate {} from profile {}", clazz.getSimpleName(), profile, e);
        }
    }

    private static InputStream findResource(Class<?> clazz, String profile) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            String fileName = String.format("/strategy/configuration/%s-%s.json", current.getSimpleName(), profile);
            InputStream is = StrategyConfigurationLoader.class.getResourceAsStream(fileName);
            if (is != null) {
                return is;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static StrategyProperties findAnnotation(Class<?> clazz) {
        if (clazz == null) return null;
        return clazz.getAnnotation(StrategyProperties.class);
    }
}

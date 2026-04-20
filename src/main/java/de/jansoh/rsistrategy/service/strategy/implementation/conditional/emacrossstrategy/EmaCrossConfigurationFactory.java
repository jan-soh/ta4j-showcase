package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class EmaCrossConfigurationFactory {

    private final ObjectMapper objectMapper;
    private static final String CONFIG_PATH = "strategy/configuration/";

    public EmaCrossConfigurationFactory() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public EmaCrossConfiguration create(String fileName) {

        if (!fileName.endsWith(".json")) {
            fileName += ".json";
        }
        String path = CONFIG_PATH + fileName;
        ClassPathResource resource = new ClassPathResource(path);

        try (InputStream inputStream = resource.getInputStream()) {

            return objectMapper.readValue(inputStream, EmaCrossConfiguration.class);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration from " + path, e);
        }
    }
}

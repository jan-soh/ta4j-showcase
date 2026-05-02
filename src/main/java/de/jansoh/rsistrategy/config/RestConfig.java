package de.jansoh.rsistrategy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for setting up REST-related beans in the application context.
 * <p>
 * This class defines and registers beans used for creating and customizing
 * REST communication and JSON processing.
 */
@Configuration
public class RestConfig {

    /**
     * Creates and registers a {@link RestTemplate} bean in the application context.
     * RestTemplate is used for synchronous client-side HTTP access, enabling
     * communication with RESTful services.
     *
     * @return a new instance of {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Creates and configures a {@link ObjectMapper} bean for use in JSON processing within the application.
     * This method customizes the {@link ObjectMapper} by registering the {@link JavaTimeModule},
     * which adds support for serializing and deserializing Java 8 date and time API types.
     *
     * @return a configured instance of {@link ObjectMapper} with the JavaTimeModule registered
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}

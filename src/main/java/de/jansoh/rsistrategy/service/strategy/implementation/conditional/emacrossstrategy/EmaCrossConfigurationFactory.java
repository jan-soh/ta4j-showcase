package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Factory class for creating instances of {@code EmaCrossConfiguration}.
 * This class handles the reading and deserialization of configuration files
 * located in the classpath. It ensures that the configuration files are properly
 * loaded and parsed into Java objects.
 * <p>
 * The factory uses {@code ObjectMapper} from the Jackson library for JSON deserialization
 * and supports configuration files with a ".json" extension.
 * <p>
 * Default configuration files are expected to be located in the
 * "strategy/configuration/" directory on the classpath.
 * <p>
 * Responsibilities:
 * - Appends the ".json" extension to the file name if it is not already included.
 * - Validates and reads the configuration file from the specified path.
 * - Deserializes the JSON content into an {@code EmaCrossConfiguration} object.
 * <p>
 * Exceptions:
 * - Throws {@code RuntimeException} if the configuration file fails to load or parse.
 */
@Component
public class EmaCrossConfigurationFactory {

    /**
     * A Jackson {@code ObjectMapper} instance used for JSON serialization and
     * deserialization purposes within the {@code EmaCrossConfigurationFactory}.
     * This object is responsible for converting JSON data into Java objects
     * and vice versa, facilitating the reading and handling of configuration
     * files in the application.
     * <p>
     * The {@code ObjectMapper} is initialized with the {@code JavaTimeModule}
     * to support proper serialization and deserialization of Java 8 date and
     * time types.
     * <p>
     * Thread-safety:
     * This instance is declared as {@code final} and should not be
     * modified after initialization, making it thread-safe under the assumption
     * of immutable usage patterns.
     */
    private final ObjectMapper objectMapper;

    /**
     * Represents the path to the directory where default configuration files for
     * the EMA Cross Strategy are stored. Configuration files are expected to be
     * located under this specified relative path within the classpath.
     * <p>
     * The path is used by the {@code EmaCrossConfigurationFactory} to construct
     * the full resource path to JSON configuration files. It serves as the base
     * directory for loading and deserializing configuration data into Java objects.
     * <p>
     * This variable is declared as {@code private static final} because it defines
     * a constant critical to the application's configuration loading process and
     * should not be modified.
     */
    private static final String CONFIG_PATH = "strategy/configuration/";

    /**
     * Constructs an instance of the {@code EmaCrossConfigurationFactory}.
     * <p>
     * This constructor initializes an {@code ObjectMapper} instance and configures it
     * by registering the {@code JavaTimeModule} to support Java 8 date and time API types
     * during serialization and deserialization.
     */
    public EmaCrossConfigurationFactory() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Creates an instance of {@code EmaCrossConfiguration} by reading a JSON configuration file.
     * If the provided file name does not end with ".json", the extension is appended automatically.
     *
     * @param fileName the name of the configuration file to read.
     *                 It may or may not include the ".json" extension.
     * @return an instance of {@code EmaCrossConfiguration} built from the contents of the configuration file
     * @throws RuntimeException if an I/O error occurs while reading the file or if the file cannot be parsed.
     */
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

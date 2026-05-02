package de.jansoh.rsistrategy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.Precision;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for managing and providing precision information for trading symbols.
 * Precision refers to the decimal and quantity precision settings for trading pairs.
 * <p>
 * This service fetches and stores the precision data for all trading symbols from
 * an external API and provides methods to access the precision of specific symbols.
 * The data is refreshed daily to ensure accuracy.
 * <p>
 * The initialization process is synchronized to avoid concurrency issues, and the
 * precision data is stored in an in-memory map for quick access.
 * <p>
 * Methods:
 * - {@link #init()}: Fetches precision data for all trading symbols and updates the internal cache.
 * - {@link #getPrecision(String)}: Retrieves the precision information for a specific symbol.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrecisionService {

    /**
     * An instance of the BinanceApiService used to interact with the Binance API.
     * This service provides methods for retrieving exchange information and other
     * trading-related data required for precision management.
     * <p>
     * The BinanceApiService is a core dependency for retrieving the precision data
     * for trading symbols and supports the population of the internal cache used by
     * the PrecisionService.
     */
    private final BinanceApiService binanceApiService;

    /**
     * An in-memory cache that stores precision information for trading symbols.
     * The keys are the trading symbol names, and the values are {@link Precision} objects
     * containing decimal and quantity precision settings for the corresponding symbols.
     * <p>
     * This map is initialized and updated during the service's initialization and data
     * refresh routine, ensuring it contains the latest precision data fetched from an
     * external source (such as an API).
     * <p>
     * It is accessed by methods within the {@link PrecisionService} class to provide
     * precision information for individual trading symbols.
     * <p>
     * The map is thread-safe for reads and updates due to synchronized access mechanisms
     * implemented in the APIs interacting with it.
     */
    private final Map<String, Precision> precisionMap = new HashMap<>();

    /**
     * An instance of {@link ObjectMapper} used for JSON serialization and deserialization.
     * This utility is utilized for converting objects to and from JSON format, such as
     * parsing API responses and converting them into domain objects or vice versa.
     * <p>
     * Within the {@link PrecisionService}, it is specifically employed to process the
     * "symbols" field from the exchange information provided by the Binance API,
     * enabling the mapping of JSON data into {@link Precision} entities.
     * <p>
     * The configuration of this object is the default provided by the Jackson library.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the precision map by fetching and processing precision data for all trading symbols
     * from the Binance API. This method clears any existing data in the precision map and repopulates it
     * with the latest information.
     * <p>
     * The method is executed once during the service startup phase (via {@link PostConstruct}) and periodically
     * every 24 hours (via {@link Scheduled}) to ensure that the cached precision data remains up-to-date.
     * <p>
     * This operation is synchronized to ensure thread safety during concurrent access or updates.
     * <p>
     * Key steps performed by this method:
     * 1. Clears the current contents of the precision map.
     * 2. Retrieves exchange information using the {@link BinanceApiService#getExchangeInfo} method.
     * 3. Extracts and deserializes the "symbols" data into an array of {@link Precision} objects.
     * 4. Iterates through the deserialized data and updates the precision map with the symbol-precision mapping.
     * 5. Logs the total number of entries in the precision map for tracking and debugging purposes.
     */
    @PostConstruct
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    public synchronized void init() {
        precisionMap.clear();
        Map<String, Object> exchangeInfo = binanceApiService.getExchangeInfo();
        Precision[] symbols = objectMapper.convertValue(exchangeInfo.get("symbols"), Precision[].class);

        for (Precision precision : symbols) {
            precisionMap.put(precision.getSymbol(), precision);
        }

        log.info("----- PrecisionService ----- Initialized precision map with {} entries", precisionMap.size());
    }

    /**
     * Retrieves the precision information for a specific trading symbol.
     * Precision includes details such as decimal and quantity precision settings
     * for the provided symbol.
     *
     * @param symbol the trading symbol for which the precision information is to be retrieved.
     *               It must exist in the precisionMap; otherwise, an exception is thrown.
     * @return the {@link Precision} object containing the precision details for the given symbol.
     * @throws PrecisionNotFoundForSymbolException if no precision information is found for the given symbol.
     */
    public synchronized Precision getPrecision(String symbol) {
        if (!precisionMap.containsKey(symbol)) {
            throw new PrecisionNotFoundForSymbolException("Precision not found for symbol: " + symbol);
        }
        return precisionMap.get(symbol);
    }
}

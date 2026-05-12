package de.jansoh.rsistrategy.service.strategy;

import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import org.springframework.beans.factory.annotation.Value;

public abstract class DefaultStrategy implements ConditionalStrategy {

    /**
     * Minimum notional value used as a threshold for trade execution.
     * This value is typically specified in the application configuration
     * using the 'binance.api.settings.notional-min' property, with a default
     * fallback value of 50 if the property is not defined.
     */
    @Value("${binance.api.settings.notional-min}:50")
    protected double notionalMin;
}

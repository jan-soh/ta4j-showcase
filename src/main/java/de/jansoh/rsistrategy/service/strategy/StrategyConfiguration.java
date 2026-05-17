package de.jansoh.rsistrategy.service.strategy;

import de.jansoh.rsistrategy.model.AssetTradeWindow;

/**
 * StrategyConfiguration serves as a blueprint for creating strategy-specific
 * configuration objects used in trading applications.
 * It defines the required methods for implementing configurations
 * that are tailored to different trading strategies.
 * <p>
 * Implementing classes are expected to define and encapsulate all the
 * parameters, filters, thresholds, and settings necessary for a specific
 * trading strategy. These configurations often include trade-related
 * settings, technical indicators, risk management parameters, and more.
 * <p>
 * The interface specifically provides a method to retrieve the associated
 * {@code AssetTradeWindow}, which defines the trading window characteristics
 * for a specific asset underlying the strategy.
 * <p>
 * Responsibilities:
 * - Enforce a consistent structure for trading strategy configurations.
 * - Provide access to asset-specific trading window settings.
 */
public interface StrategyConfiguration {

    /**
     * Retrieves the trading window configuration for a specific asset.
     * <p>
     * The returned {@code AssetTradeWindow} provides detailed information
     * about the trading symbol, timeframe, and leverage settings used for
     * executing and managing trades in the context of the current trading
     * strategy. This configuration is essential for aligning the strategy
     * with the specific characteristics of the asset being traded.
     *
     * @return an {@code AssetTradeWindow} object representing the trading
     * window parameters, including the asset symbol, timeframe, and leverage.
     */
    AssetTradeWindow getAssetTradeWindow();

    /**
     * Indicates whether multiple active positions are allowed within the trading window.
     *
     * @return true if multiple active positions are allowed, false otherwise.
     */
    boolean isMultipleActivePositionsAllowed();
}

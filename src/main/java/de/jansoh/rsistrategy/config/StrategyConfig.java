package de.jansoh.rsistrategy.config;

import de.jansoh.rsistrategy.strategy.EmaCrossStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;

@Configuration
public class StrategyConfig {

    @Value("${strategy.symbol:BTCUSDT}")
    private String symbol;

    @Value("${strategy.emaTriggerLength:50}")
    private int emaTriggerLength;

    @Value("${strategy.emaFilterLength:200}")
    private int emaFilterLength;

    @Value("${strategy.atrLength:14}")
    private int atrLength;

    @Bean
    public BarSeries barSeries() {
        return new BaseBarSeriesBuilder().withName(symbol).build();
    }

    @Bean
    public Strategy strategy(BarSeries barSeries) {
        return EmaCrossStrategy.buildStrategy(barSeries, emaTriggerLength, emaFilterLength, true, true, true, true);
    }

    @Bean
    public ATRIndicator atrIndicator(BarSeries barSeries) {
        return new ATRIndicator(barSeries, atrLength);
    }
}

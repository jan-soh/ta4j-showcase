package de.jansoh.rsistrategy.service.strategy.implementation;

import de.jansoh.rsistrategy.service.strategy.tpsl.SlStrategy;
import de.jansoh.rsistrategy.service.strategy.tpsl.TpStrategy;
import org.ta4j.core.Strategy;

public interface AdvancedStrategy extends TpStrategy, SlStrategy {

    Strategy getStrategy();
}

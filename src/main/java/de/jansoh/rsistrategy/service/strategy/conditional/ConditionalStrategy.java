package de.jansoh.rsistrategy.service.strategy.conditional;

import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.service.strategy.StrategyConfiguration;
import de.jansoh.rsistrategy.service.strategy.tpsl.SlStrategy;
import de.jansoh.rsistrategy.service.strategy.tpsl.TpStrategy;

public interface ConditionalStrategy extends TpStrategy, SlStrategy {

    boolean isLongEntrySatisfied(int index);

    boolean isLongExitSatisfied(int index, Position position);

    boolean isShortEntrySatisfied(int index);

    boolean isShortExitSatisfied(int index, Position position);

    StrategyConfiguration getConfiguration();
}

package de.jansoh.rsistrategy.service.strategy.conditional;

import de.jansoh.rsistrategy.service.strategy.tpsl.SlStrategy;
import de.jansoh.rsistrategy.service.strategy.tpsl.TpStrategy;
import org.ta4j.core.TradingRecord;

public interface ConditionalStrategy extends TpStrategy, SlStrategy {

    boolean isLongEntrySatisfied(int index, TradingRecord tradingRecord);

    boolean isLongExitSatisfied(int index, TradingRecord tradingRecord);

    boolean isShortEntrySatisfied(int index, TradingRecord tradingRecord);

    boolean isShortExitSatisfied(int index, TradingRecord tradingRecord);
}

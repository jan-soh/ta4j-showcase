package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;

public class FastEmaCrossingSlowEmaStrategy implements ConditionalStrategy {

    private final BarSeries barSeries;

    private EmaCrossConfiguration configuration;
    private EmaCrossLongRules longRules;


    public FastEmaCrossingSlowEmaStrategy(BarSeries barSeries) {
        this.barSeries = barSeries;
        configuration = new EmaCrossConfiguration();
        longRules = new EmaCrossLongRules(configuration, barSeries);
    }

    public boolean isLongEntrySatisfied(int index, TradingRecord tradingRecord) {
        return longRules.getEntryRule().isSatisfied(index, tradingRecord);
    }

    public boolean isLongExitSatisfied(int index, TradingRecord tradingRecord) {
        return longRules.getEntryRule().isSatisfied(index, tradingRecord);
    }

    @Override
    public boolean isShortEntrySatisfied(int index, TradingRecord tradingRecord) {
        return false;
    }

    @Override
    public boolean isShortExitSatisfied(int index, TradingRecord tradingRecord) {
        return false;
    }

    @Override
    public BigDecimal getSl(Bar positionEntry, Position position) {

        Num height = positionEntry.getClosePrice()
                .minus(positionEntry.getOpenPrice())
                .multipliedBy(DecimalNum.valueOf(configuration.getSlMultiplier()))
                .abs();

        if (PositionSide.LONG == position.getSide()) {
            return new BigDecimal(positionEntry.getOpenPrice().minus(height).toString());
        }
        return new BigDecimal(positionEntry.getOpenPrice().plus(height).toString());
    }


    /**
     * Calculates the take-profit (TP) level for the given position based on its side.
     *
     * @param positionEntry the entry bar of the position, containing market data at the time of entry
     * @param position      the position for which the take-profit level is to be calculated
     * @return the take-profit level as a {@code BigDecimal}; returns {@code Long.MAX_VALUE} for long positions
     * and {@code BigDecimal.ZERO} for non-long positions - this strategy uses conditional TP levels.
     */
    @Override
    public BigDecimal getTp(Bar positionEntry, Position position) {
        if (PositionSide.LONG == position.getSide()) {
            return new BigDecimal(Long.MAX_VALUE);
        }
        return BigDecimal.ZERO;
    }
}

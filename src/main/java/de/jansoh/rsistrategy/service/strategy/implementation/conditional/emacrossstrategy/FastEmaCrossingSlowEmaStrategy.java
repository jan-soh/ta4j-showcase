package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import de.jansoh.rsistrategy.service.strategy.StrategyConfiguration;
import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import de.jansoh.rsistrategy.ta4jbridge.model.TsPosition;
import de.jansoh.rsistrategy.ta4jbridge.model.TsTrade;
import de.jansoh.rsistrategy.ta4jbridge.model.TsTradingRecord;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FastEmaCrossingSlowEmaStrategy implements ConditionalStrategy {

    private final BarSeries barSeries;

    private EmaCrossConfiguration configuration;
    private EmaCrossLongRules longRules;
    private EmaCrossShortRules shortRules;


    public FastEmaCrossingSlowEmaStrategy(BarSeries barSeries) {
        this.barSeries = barSeries;
        configuration = new EmaCrossConfiguration();
        configuration.setDefaults();
        longRules = new EmaCrossLongRules(configuration, barSeries);
        shortRules = new EmaCrossShortRules(configuration, barSeries);
    }

    public FastEmaCrossingSlowEmaStrategy(BarSeries barSeries, EmaCrossConfiguration configuration) {
        this.barSeries = barSeries;
        this.configuration = configuration;
        longRules = new EmaCrossLongRules(configuration, barSeries);
        shortRules = new EmaCrossShortRules(configuration, barSeries);
    }

    @Override
    public boolean isLongEntrySatisfied(int index) {

        TsTradingRecord tradingRecord = new TsTradingRecord(Integer.toString(index), TsTrade.TradeType.BUY);
        return longRules.getEntryRule().isSatisfied(index, tradingRecord);
    }

    @Override
    public boolean isLongExitSatisfied(int index, Position position) {

        TsTrade entryTrade = new TsTrade(position.getEntryIndex(), barSeries, TsTrade.TradeType.BUY);
        TsPosition tsPosition = new TsPosition(entryTrade);
        TsTradingRecord tradingRecord = new TsTradingRecord(tsPosition);

        return longRules.getExitRule().isSatisfied(index, tradingRecord);
    }

    @Override
    public boolean isShortEntrySatisfied(int index) {

        TsTradingRecord tradingRecord = new TsTradingRecord(Integer.toString(index), TsTrade.TradeType.SELL);
        return shortRules.getEntryRule().isSatisfied(index, tradingRecord);
    }

    @Override
    public boolean isShortExitSatisfied(int index, Position position) {

        TsTrade entryTrade = new TsTrade(position.getEntryIndex(), barSeries, TsTrade.TradeType.SELL);
        TsPosition tsPosition = new TsPosition(entryTrade);
        TsTradingRecord tradingRecord = new TsTradingRecord(tsPosition);

        return shortRules.getExitRule().isSatisfied(index, tradingRecord);
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
            return positionEntry.getOpenPrice().multipliedBy(DecimalNum.valueOf(2.0)).bigDecimalValue();
        }
        // TP of zero does not work for many APIs. 10% of the entry price should do it.
        // Plus Binance also requires orders to have a notional value of at least 50 (USDT) -> quantity * price = notational value
        BigDecimal price;
        BigDecimal bestPrice = positionEntry.getOpenPrice().multipliedBy(DecimalNum.valueOf(0.1)).bigDecimalValue();
        BigDecimal minPrice = BigDecimal.valueOf(50.000).divide(position.getQuantity(), RoundingMode.HALF_UP);
        if (bestPrice.compareTo(minPrice) < 0) {
            return minPrice;
        } else {
            return bestPrice;
        }
    }

    @Override
    public StrategyConfiguration getConfiguration() {
        return this.configuration;
    }
}

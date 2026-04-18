package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanRule;

import java.time.ZoneId;

public abstract class EmaCrossRules {

    protected final EmaCrossConfiguration configuration;
    protected final BarSeries barSeries;

    protected ClosePriceIndicator closePrice;
    protected VolumeIndicator volume;
    protected HighPriceIndicator highPrice;
    protected LowPriceIndicator lowPrice;

    protected EMAIndicator ema20;
    protected EMAIndicator ema50;
    protected EMAIndicator ema200;

    protected MACDIndicator macdLine;

    protected RSIIndicator rsi;

    protected SMAIndicator avgVolume;

    protected Rule volumeMet;

    protected Rule allowEntryDate;

    protected EmaCrossRules(EmaCrossConfiguration configuration, BarSeries barSeries) {

        this.configuration = configuration;
        this.barSeries = barSeries;

        this.closePrice = new ClosePriceIndicator(barSeries);
        this.volume = new VolumeIndicator(barSeries);
        this.highPrice = new HighPriceIndicator(barSeries);
        this.lowPrice = new LowPriceIndicator(barSeries);

        this.ema20 = new EMAIndicator(closePrice, configuration.getEma20Length());
        this.ema50 = new EMAIndicator(closePrice, configuration.getEma50Length());
        this.ema200 = new EMAIndicator(closePrice, configuration.getEma200Length());

        this.macdLine = new MACDIndicator(closePrice, configuration.getMacdFastLength(), configuration.getMacdSlowLength());

        this.rsi = new RSIIndicator(closePrice, 14);

        // 4. Volume Filter
        this.avgVolume = new SMAIndicator(volume, configuration.getVolAvgPeriod());

        // Rules
        // volume > volMultiplier * avgVolume
        this.volumeMet = configuration.isUseVolumeFilter() ?
                (index, tradingRecord) -> {
                    Num vol = volume.getValue(index);
                    Num avg = avgVolume.getValue(index);
                    return vol.isGreaterThan(avg.multipliedBy(barSeries.numFactory().numOf(configuration.getVolMultiplier())));
                } :
                new BooleanRule(true);

        allowEntryDate = (index, tradingRecord) -> barSeries.getBar(index).getBeginTime().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(configuration.getEntryDate());
    }

    protected static double calculateAngle(int index, EMAIndicator ema20) {
        Num current = ema20.getValue(index);
        Num previous = ema20.getValue(index - 1);
        double slope = current.minus(previous).dividedBy(previous).multipliedBy(ema20.getBarSeries().numFactory().numOf(100)).doubleValue();
        return Math.atan(slope) * 180.0 / Math.PI;
    }

    public abstract Rule getExitRule();

    public abstract Rule getEntryRule();
}

package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.service.strategy.StrategyConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmaCrossConfiguration implements StrategyConfiguration {

    protected int ema20Length;
    protected int ema50Length;
    protected int ema200Length;
    protected boolean useEma200Filter;
    protected boolean useRsiFilter;
    protected int rsiThreshold;
    protected boolean useMacdFilter;
    protected double macdThreshold;
    protected int macdFastLength;
    protected int macdSlowLength;
    protected boolean useVolumeFilter;
    protected double volMultiplier;
    protected int volAvgPeriod;
    protected boolean useEma200DistanceFilter;
    protected double ema200DistPerc;
    protected boolean useEmaSlopeFilter;
    protected double emaSlopeThreshold;
    protected double tpUndercutPerc;
    protected double slMultiplier;
    protected boolean allowLong;
    protected boolean allowShort;

    @JsonProperty("assetTradeWindow")
    private AssetTradeWindow assetTradeWindow;

    /**
     * Only used for testing purposes. Use this to allow trades only after a specific date.
     */
    protected LocalDate entryDate;

    public void setDefaults() {
        this.ema20Length = 20;
        this.ema50Length = 50;
        this.ema200Length = 200;
        this.useEma200Filter = true;
        this.useRsiFilter = false;
        this.rsiThreshold = 50;
        this.useMacdFilter = false;
        this.macdThreshold = 0.0;
        this.macdFastLength = 12;
        this.macdSlowLength = 26;
        this.useVolumeFilter = true;
        this.volMultiplier = 2.0;
        this.volAvgPeriod = 8;
        this.useEma200DistanceFilter = true;
        this.ema200DistPerc = 1.5;
        this.useEmaSlopeFilter = false;
        this.emaSlopeThreshold = 0.01;
        this.tpUndercutPerc = 0.25;
        this.slMultiplier = 2.0;
        this.allowLong = true;
        this.allowShort = true;
        this.entryDate = LocalDate.of(2025, 9, 4);
        this.assetTradeWindow = AssetTradeWindow.builder()
                .symbol("BTCUSDT")
                .timeframe(Timeframe.FIFTEEN_MINUTES)
                .leverage(5)
                .build();
    }
}

package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class EmaCrossConfiguration {

    private int ema20Length;
    private int ema50Length;
    private int ema200Length;
    private boolean useEma200Filter;
    private boolean useRsiFilter;
    private int rsiThreshold;
    private boolean useMacdFilter;
    private double macdThreshold;
    private int macdFastLength;
    private int macdSlowLength;
    private boolean useVolumeFilter;
    private double volMultiplier;
    private int volAvgPeriod;
    private boolean useEma200DistanceFilter;
    private double ema200DistPerc;
    private boolean useEmaSlopeFilter;
    private double emaSlopeThreshold;
    private double tpUndercutPerc;
    private double slMultiplier;
    private boolean allowLong;
    private boolean allowShort;

    /**
     * Only used for testing purposes. Use this to allow trades only after a specific date.
     */
    private LocalDate entryDate;

    public EmaCrossConfiguration() {
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
    }
}

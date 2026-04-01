package de.jansoh.rsistrategy.model;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class Position {
    private String type; // LONG/SHORT
    private ZonedDateTime openDate;
    private double entryPrice;
    private double stopLoss;
    private double takeProfit;
    private boolean closed;
    private ZonedDateTime closeDate;
    private double exitPrice;
}

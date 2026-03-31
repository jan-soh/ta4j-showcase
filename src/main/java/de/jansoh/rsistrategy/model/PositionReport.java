package de.jansoh.rsistrategy.model;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
public class PositionReport {
    private String type; // long/short
    private ZonedDateTime openDate;
    private double entryPrice;
    private ZonedDateTime closeDate;
    private double exitPrice;
    private double pnl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    public String toString() {
        return String.format("%s,%s,%.2f,%s,%.2f,%.2f",
                type,
                openDate.format(FORMATTER),
                entryPrice,
                closeDate != null ? closeDate.format(FORMATTER) : "Open",
                exitPrice,
                pnl);
    }
}

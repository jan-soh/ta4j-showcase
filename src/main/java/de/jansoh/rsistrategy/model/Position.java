package de.jansoh.rsistrategy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // LONG/SHORT
    private ZonedDateTime openDate;
    private double entryPrice;
    private double stopLoss;
    private double takeProfit;
    private boolean closed;
    private ZonedDateTime closeDate;
    private double exitPrice;
}

package de.jansoh.rsistrategy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private String binanceOrderId;
    private String tpAlgoId;
    private String slAlgoId;
}

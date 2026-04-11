package de.jansoh.rsistrategy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "trade_position")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private PositionSide side;

    @Column(precision = 20, scale = 10)
    private BigDecimal quantity;

    @Column(precision = 20, scale = 10)
    private BigDecimal averageOpenPrice;

    @Column(precision = 20, scale = 10)
    private BigDecimal averageClosedPrice;

    @Column(precision = 20, scale = 10)
    private BigDecimal realizedProfit;

    private ZonedDateTime openTime;

    private ZonedDateTime closedTime;

    private boolean closed;
}

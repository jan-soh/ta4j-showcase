package de.jansoh.rsistrategy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;


@Entity
@Table(name = "trade_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientOrderId;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private Timeframe timeframe;

    @Enumerated(EnumType.STRING)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    private PositionSide positionSide;

    @Enumerated(EnumType.STRING)
    private OrderType type;

    @Column(precision = 20, scale = 10)
    private BigDecimal originalQuantity;

    @Column(precision = 20, scale = 10)
    private BigDecimal lastFilledQuantity;

    @Column(precision = 20, scale = 10)
    private BigDecimal filledAccumulatedQuantity;

    @Column(precision = 20, scale = 10)
    private BigDecimal originalPrice;

    @Column(precision = 20, scale = 10)
    private BigDecimal averagePrice;

    @Column(precision = 20, scale = 10)
    private BigDecimal lastFilledPrice;

    @Column(precision = 20, scale = 10)
    private BigDecimal realizedProfit;

    private String commissionAsset;

    @Column(precision = 20, scale = 10)
    private BigDecimal commission;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private String orderId;

    private ZonedDateTime orderTradeTime;
}

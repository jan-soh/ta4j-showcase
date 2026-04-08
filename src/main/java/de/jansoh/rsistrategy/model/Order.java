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

    private String symbol;
    private OrderSide side;
    private PositionSide positionSide;
    private OrderType type;
    private BigDecimal originalQuantity;
    private BigDecimal lastFilledQuantity;
    private BigDecimal filledAccumulatedQuantity;
    private BigDecimal originalPrice;
    private BigDecimal averagePrice;
    private BigDecimal lastFilledPrice;
    private BigDecimal realizedProfit;
    private String commissionAsset;
    private BigDecimal commission;
    private OrderStatus orderStatus;
    private String orderId;
    private ZonedDateTime orderTradeTime;


}

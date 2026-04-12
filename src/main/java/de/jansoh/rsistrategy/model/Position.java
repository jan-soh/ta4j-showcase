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
    private Timeframe timeframe;

    @Enumerated(EnumType.STRING)
    private PositionSide side;

    @Builder.Default
    @Column(precision = 20, scale = 10)
    private BigDecimal quantity = BigDecimal.ZERO;

    private String tpAlgoOrderId;
    private String tpClientOrderId;

    @Column(precision = 20, scale = 10)
    private BigDecimal tpAlgoPrice;

    private String slAlgoOrderId;
    private String slClientOrderId;

    @Column(precision = 20, scale = 10)
    private BigDecimal slAlgoPrice;

    @Column(precision = 20, scale = 10)
    private BigDecimal averageOpenPrice;

    @Column(precision = 20, scale = 10)
    private BigDecimal averageClosedPrice;

    @Builder.Default
    @Column(precision = 20, scale = 10)
    private BigDecimal realizedProfit = BigDecimal.ZERO;

    private ZonedDateTime openTime;

    private ZonedDateTime closedTime;

    private boolean closed;

    public boolean hasTpAlgoOrder() {
        return tpAlgoOrderId != null;
    }

    public boolean hasSlAlgoOrder() {
        return slAlgoOrderId != null;
    }

    public void addRealizedProfit(BigDecimal realizedProfit) {
        if (null == realizedProfit) {
            return;
        }
        if (null == this.realizedProfit) {
            this.realizedProfit = BigDecimal.ZERO;
        }

        this.realizedProfit = this.realizedProfit.add(realizedProfit);
    }
}

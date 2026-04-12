package de.jansoh.rsistrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoOrder {

    private String orderId;
    private String clientOrderId;
    private String symbol;
    private BigDecimal quantity;
    private Timeframe timeframe;
    private String algoId;
    private AlgoOrderType type;
    private BigDecimal triggerPrice;

}

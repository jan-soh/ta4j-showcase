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

    private String symbol;
    private String algoId;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal triggerPrice;

}

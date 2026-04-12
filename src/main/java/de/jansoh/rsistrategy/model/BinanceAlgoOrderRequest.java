package de.jansoh.rsistrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinanceAlgoOrderRequest {
    private String algoType;
    private String symbol;
    private String side;
    private String positionSide;
    private String type; // STOP_LOSS, TAKE_PROFIT, etc.
    private String triggerPrice;
    private String quantity;
    private String stopPrice;
    private String workingType;
    private String priceProtect;
    private String closePosition;
    private Long recvWindow;
    private String clientAlgoId;
    private Long timestamp;
}

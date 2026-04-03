package de.jansoh.rsistrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinanceOrderRequest {
    private String symbol;
    private String side;
    private String positionSide;
    private String type;
    private String reduceOnly;
    private String quantity;
    private String price;
    private String newClientOrderId;
    private String stopPrice;
    private String closePosition;
    private String activationPrice;
    private String callbackRate;
    private String timeInForce;
    private String workingType;
    private String priceProtect;
    private String responseType;
    private Long recvWindow;
    private Long timestamp;
}

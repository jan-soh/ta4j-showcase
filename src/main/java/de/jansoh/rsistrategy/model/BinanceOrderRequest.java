package de.jansoh.rsistrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a request payload for placing an order on Binance.
 * This class encapsulates the essential information required to construct and send
 * an order request to the Binance API. It supports various order types, positions,
 * and configurations applicable to spot and futures markets.
 * <p>
 * Fields in this class correspond to the parameters of the Binance API for placing orders,
 * including symbol, side, type, and other optional attributes for fine-tuning order execution.
 * <p>
 * The attributes include:
 * - Basic order details like trading pair symbol, order side (e.g., buy/sell), and order type.
 * - Position details like position side (e.g., long/short) and close position flag.
 * - Pricing details such as quantity, price, stop price, activation price, and callback rate.
 * - Configuration options including time-in-force, reduce-only flag, price protection, and response type.
 * - Metadata for tracking and request integrity like client order ID, timestamp, and receive window.
 * <p>
 * This class is annotated with Lombok annotations to avoid boilerplate code:
 * - @Data generates getter, setter, toString, equals, and hashCode methods.
 * - @Builder facilitates fluent construction of instances.
 * - @NoArgsConstructor and @AllArgsConstructor provide constructors with no arguments and all arguments respectively.
 *
 * @see <a href="https://developers.binance.com/docs/derivatives/usds-margined-futures/trade/rest-api">New Order (Trade)</a>
 */
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
    private String priceMatch;
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

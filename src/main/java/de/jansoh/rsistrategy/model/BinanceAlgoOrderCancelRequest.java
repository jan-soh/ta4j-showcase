package de.jansoh.rsistrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a request to cancel an algorithmic order on Binance.
 * This class is used to specify the details necessary to cancel
 * an existing algorithmic order.
 * <p>
 * The cancellation request can be identified either by the algorithmic
 * order ID (algoId) or the client-assigned algorithmic order ID (clientAlgoId).
 * Additional parameters like recvWindow and timestamp are used for
 * request validation and ordering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinanceAlgoOrderCancelRequest {

    /**
     * The unique identifier for an algorithmic order.
     * <p>
     * This identifier is used to specify a particular algorithmic order
     * when requesting cancellation or performing operations on such orders.
     * It corresponds to the system-generated ID associated with the order
     * and should be used when `clientAlgoId` is not specified.
     */
    private Long algoId;

    /**
     * The client-defined identifier for an algorithmic order.
     * <p>
     * The client assigns this identifier to track and manage
     * algorithmic orders independently of the system-generated `algoId`.
     * It can be used to uniquely identify a specific algorithmic order
     * when requesting cancellation or performing other related operations.
     */
    private String clientAlgoId;

    /**
     * The maximum allowable duration, in milliseconds, within which the request
     * is considered valid by the Binance API server.
     *
     * <p>
     * If the difference between the current server time and the timestamp of the
     * request exceeds this value, the server will reject the request.
     * This parameter is optional and can be used to define a custom time window
     * for ensuring the request timeliness when communicating with the Binance API.
     * </p>
     */
    private Long recvWindow;

    /**
     * The timestamp of the request in milliseconds since the Unix epoch.
     * <p>
     * This field is used for request validation and ordering by ensuring that
     * the request's timing aligns with the server's expectations. It represents
     * the exact moment when the request is created and should closely match the
     * system time of the client issuing the request.
     * <p>
     * The Binance API uses this value to check the freshness of the request and
     * will reject requests where the timestamp deviates too significantly from the
     * server's current time, unless `recvWindow` is specified to allow for such
     * deviations.
     */
    private Long timestamp;
}

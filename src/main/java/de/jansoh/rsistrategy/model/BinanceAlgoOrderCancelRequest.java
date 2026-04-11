package de.jansoh.rsistrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request parameters for cancelling an algo order.
 * Either algoId or clientAlgoId must be sent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinanceAlgoOrderCancelRequest {
    private Long algoId;
    private String clientAlgoId;
    private Long recvWindow;
    private Long timestamp;
}

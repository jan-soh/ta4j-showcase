package de.jansoh.rsistrategy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a message containing Kline (candlestick) data received from the Binance API.
 * This data is typically used for charting and analyzing market trends over specific intervals.
 * <p>
 * The message contains the following key parts:
 * - General message metadata including event type and event time.
 * - The symbol (e.g., trading pair) for which the Kline data is relevant.
 * - A nested KlineData object containing detailed candlestick information such as
 * open, close, high, and low prices, volume, and trade counts.
 *
 * @see <a href="https://developers.binance.com/docs/derivatives/usds-margined-futures/websocket-market-streams/Kline-Candlestick-Streams">Kline-Candlestick-Streams</a>
 */
@Data
public class BinanceKlineMessage {
    @JsonProperty("e")
    private String eventType;
    @JsonProperty("E")
    private Long eventTime;
    @JsonProperty("s")
    private String symbol;
    @JsonProperty("k")
    private KlineData kline;

    @Data
    public static class KlineData {
        @JsonProperty("t")
        private Long startTime;
        @JsonProperty("T")
        private Long closeTime;
        @JsonProperty("s")
        private String symbol;
        @JsonProperty("i")
        private String interval;
        @JsonProperty("f")
        private Long firstTradeId;
        @JsonProperty("L")
        private Long lastTradeId;
        @JsonProperty("o")
        private String openPrice;
        @JsonProperty("c")
        private String closePrice;
        @JsonProperty("h")
        private String highPrice;
        @JsonProperty("l")
        private String lowPrice;
        @JsonProperty("v")
        private String volume;
        @JsonProperty("n")
        private Integer numberOfTrades;
        @JsonProperty("x")
        private Boolean isClosed;
        @JsonProperty("q")
        private String quoteAssetVolume;
        @JsonProperty("V")
        private String takerBuyBaseAssetVolume;
        @JsonProperty("Q")
        private String takerBuyQuoteAssetVolume;
        @JsonProperty("B")
        private String ignore;
    }
}

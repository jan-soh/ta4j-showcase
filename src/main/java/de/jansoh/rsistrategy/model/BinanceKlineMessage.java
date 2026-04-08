package de.jansoh.rsistrategy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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

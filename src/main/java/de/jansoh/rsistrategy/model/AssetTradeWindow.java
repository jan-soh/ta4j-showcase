package de.jansoh.rsistrategy.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetTradeWindow {

    private String symbol;
    private Timeframe timeframe;

    @EqualsAndHashCode.Exclude
    private int leverage;
}

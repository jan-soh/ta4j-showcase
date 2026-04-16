package de.jansoh.rsistrategy.service.kline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.service.BinanceApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BinanceKlinesProviderFactory {

    @Value("${binance.use-real-api}")
    private boolean isRealApi;

    @Value("${trade.api.websocket.real.url}")
    private String realWebsocketApiUrl;

    @Value("${trade.api.websocket.test.url}")
    private String testWebsocketApiUrl;


    private final BinanceApiService binanceApiService;
    private final ObjectMapper objectMapper;

    public BinanceKlinesProvider create(AssetTradeWindow tradeWindow) {

        String websocketApiUrl = isRealApi ? realWebsocketApiUrl : testWebsocketApiUrl;

        return new BinanceKlinesProvider(tradeWindow, websocketApiUrl, binanceApiService, objectMapper);
    }
}

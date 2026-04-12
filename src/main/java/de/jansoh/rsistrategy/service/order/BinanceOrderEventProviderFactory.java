package de.jansoh.rsistrategy.service.order;

import de.jansoh.rsistrategy.service.BinanceApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class BinanceOrderEventProviderFactory {

    @Value("${binance.use-real-api}")
    private boolean isRealApi;

    @Value("${trade.api.websocket.real.url}")
    private String realWebsocketApiUrl;

    @Value("${trade.api.websocket.test.url}")
    private String testWebsocketApiUrl;


    private final BinanceApiService binanceApiService;
    private final ObjectMapper objectMapper;
    private final OrderUpdateEventMapper orderUpdateEventMapper;

    public BinanceOrderEventProvider create() {

        String websocketApiUrl = isRealApi ? realWebsocketApiUrl : testWebsocketApiUrl;

        BinanceOrderEventProvider provider = new BinanceOrderEventProvider(websocketApiUrl, binanceApiService, objectMapper, orderUpdateEventMapper);
        provider.init();

        return provider;
    }
}

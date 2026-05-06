package de.jansoh.rsistrategy.service.position;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.repository.OrderRepository;
import de.jansoh.rsistrategy.repository.PositionRepository;
import de.jansoh.rsistrategy.service.PrecisionService;
import de.jansoh.rsistrategy.service.TelegramMessagingService;
import de.jansoh.rsistrategy.service.broker.ApiConfiguration;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProvider;
import de.jansoh.rsistrategy.service.order.OrderUpdateEventMapper;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.FastEmaCrossingSlowEmaStrategy;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DecimalNumFactory;

import java.math.BigDecimal;

@ActiveProfiles("demo-local")
@SpringBootTest(classes = {
        ApiConfiguration.class,
        BinanceApiService.class,
        PositionService.class,
        RestTemplate.class,
        ObjectMapper.class,
        OrderUpdateEventMapper.class,
        OpenPositionRegistry.class,
        BinanceOrderEventProvider.class,
        PrecisionService.class
})
@Disabled
class PositionServiceIT {

    @Autowired
    PositionService positionService;

    @Autowired
    OpenPositionRegistry openPositionRegistry;

    @MockitoBean
    OrderRepository orderRepository;

    @MockitoBean
    PositionRepository positionRepository;

    @MockitoBean
    TelegramMessagingService telegramMessagingService;

    @Test
    void createPositionWithTpSlAndClose() throws InterruptedException {

        BarSeries barSeries = Mockito.mock(BarSeries.class);
        Mockito.when(barSeries.numFactory()).thenReturn(DecimalNumFactory.getInstance());
        FastEmaCrossingSlowEmaStrategy strategy = new FastEmaCrossingSlowEmaStrategy(barSeries);

        BigDecimal openPrice = BigDecimal.valueOf(78727.333333);
        BigDecimal closePrice = BigDecimal.valueOf(78600);
        Bar positionEntry = Mockito.mock(Bar.class);
        Mockito.when(positionEntry.getOpenPrice()).thenReturn(DecimalNum.valueOf(openPrice));
        Mockito.when(positionEntry.getClosePrice()).thenReturn(DecimalNum.valueOf(closePrice));

        Position position = Position.builder()
                .symbol("BTCUSDT")
                .quantity(BigDecimal.valueOf(0.001))
                .side(PositionSide.LONG)
                .timeframe(Timeframe.ONE_MINUTE)
                .build();

        position.setTpAlgoPrice(strategy.getTp(positionEntry, position));
        position.setSlAlgoPrice(strategy.getSl(positionEntry, position));

        positionService.init();
        Thread.sleep(5000);
        positionService.createPositionWithTpSl(position, true);
        int count = 15;
        while (count-- > 0) {
            Thread.sleep(500);
        }
        positionService.closeMarketPosition(position);
        count = 15;
        while (count-- > 0) {
            Thread.sleep(500);
        }

        System.out.println("done");
    }

    @Test
    void createPositionWithTpAndWaitForIt() throws InterruptedException {

        BarSeries barSeries = Mockito.mock(BarSeries.class);
        Mockito.when(barSeries.numFactory()).thenReturn(DecimalNumFactory.getInstance());
        FastEmaCrossingSlowEmaStrategy strategy = new FastEmaCrossingSlowEmaStrategy(barSeries);

        BigDecimal openPrice = BigDecimal.valueOf(81510);
        BigDecimal closePrice = openPrice.subtract(BigDecimal.valueOf(100));
        Bar positionEntry = Mockito.mock(Bar.class);
        Mockito.when(positionEntry.getOpenPrice()).thenReturn(DecimalNum.valueOf(openPrice));
        Mockito.when(positionEntry.getClosePrice()).thenReturn(DecimalNum.valueOf(closePrice));

        Position position = Position.builder()
                .symbol("BTCUSDT")
                .quantity(BigDecimal.valueOf(0.003))
                .side(PositionSide.LONG)
                .timeframe(Timeframe.ONE_MINUTE)
                .build();

        position.setTpAlgoPrice(openPrice.add(BigDecimal.valueOf(255)));
        position.setSlAlgoPrice(openPrice.subtract(BigDecimal.valueOf(55)));

        positionService.init();
        Thread.sleep(5000);
        positionService.createPositionWithTpSl(position, false);
        int count = 15;
        while (count-- > 0) {
            Thread.sleep(500);
        }
        AssetTradeWindow atr = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).build();
        Position p = openPositionRegistry.getPositions(atr).get(0);
        while (!p.isClosed()) {
            Thread.sleep(100);
        }

        System.out.println("done");
    }
}
package de.jansoh.rsistrategy.service.position;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.repository.OrderRepository;
import de.jansoh.rsistrategy.repository.PositionRepository;
import de.jansoh.rsistrategy.service.BinanceApiService;
import de.jansoh.rsistrategy.service.PrecisionService;
import de.jansoh.rsistrategy.service.TelegramMessagingService;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProvider;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProviderFactory;
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

@ActiveProfiles("test")
@SpringBootTest(classes = {
        BinanceApiService.class,
        PositionService.class,
        RestTemplate.class,
        ObjectMapper.class,
        OrderUpdateEventMapper.class,
        OpenPositionRegistry.class,
        BinanceOrderEventProviderFactory.class,
        PrecisionService.class
})
@Disabled
class PositionServiceIT {

    @Autowired
    PositionService positionService;

    @MockitoBean
    OrderRepository orderRepository;

    @Autowired
    BinanceOrderEventProviderFactory orderEventProviderFactory;

    @MockitoBean
    PositionRepository positionRepository;

    @MockitoBean
    TelegramMessagingService telegramMessagingService;

    @Test
    void createPositionWithTpSlAndClose() throws InterruptedException {

        BarSeries barSeries = Mockito.mock(BarSeries.class);
        Mockito.when(barSeries.numFactory()).thenReturn(DecimalNumFactory.getInstance());
        FastEmaCrossingSlowEmaStrategy strategy = new FastEmaCrossingSlowEmaStrategy(barSeries);

        BigDecimal openPrice = BigDecimal.valueOf(77979.333333);
        BigDecimal closePrice = BigDecimal.valueOf(78979.333333);
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

        BinanceOrderEventProvider orderEventProvider = orderEventProviderFactory.create();
        orderEventProvider.addOrderUpdateEventListener(positionService);
        orderEventProvider.start();
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
}
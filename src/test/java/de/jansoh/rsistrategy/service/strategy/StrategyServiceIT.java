package de.jansoh.rsistrategy.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.repository.OrderRepository;
import de.jansoh.rsistrategy.repository.PositionRepository;
import de.jansoh.rsistrategy.service.PrecisionService;
import de.jansoh.rsistrategy.service.TelegramMessagingService;
import de.jansoh.rsistrategy.service.broker.ApiConfiguration;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import de.jansoh.rsistrategy.service.indicator.EMAIndicatorFactory;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProvider;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProviderFactory;
import de.jansoh.rsistrategy.service.kline.KlinesUpdateEvent;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProvider;
import de.jansoh.rsistrategy.service.order.OrderUpdateEventMapper;
import de.jansoh.rsistrategy.service.position.OpenPositionRegistry;
import de.jansoh.rsistrategy.service.position.PositionService;
import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.EmaCrossConfiguration;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.EmaCrossConfigurationFactory;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.FastEmaCrossingSlowEmaStrategyFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@ActiveProfiles("prod-local")
@SpringBootTest(classes = {
        StrategyService.class,
        PositionService.class,
        RestTemplate.class,
        ObjectMapper.class,
        OrderUpdateEventMapper.class,
        OpenPositionRegistry.class,
        ApiConfiguration.class,
        BinanceApiService.class,
        BinanceOrderEventProvider.class,
        PrecisionService.class,
})
@Disabled
class StrategyServiceIT {

    @MockitoBean
    BinanceKlinesProviderFactory binanceKlinesProviderFactory;

    @MockitoBean
    TelegramMessagingService telegramMessagingService;

    @MockitoBean
    FastEmaCrossingSlowEmaStrategyFactory strategyFactory;

    @MockitoBean
    EMAIndicatorFactory emaIndicatorFactory;

    @MockitoBean
    OrderRepository orderRepository;

    @MockitoBean
    PositionRepository positionRepository;

    @MockitoBean
    EmaCrossConfigurationFactory strategyConfigurationFactory;

    @Autowired
    StrategyService strategyService;

    @Test
    void checkStrategy_AndValidateRealPositions2() throws InterruptedException {

        // start()

        ReflectionTestUtils.setField(strategyService, "strategiesToCreate", "config1.json");

        AssetTradeWindow assetTradeWindow = AssetTradeWindow.builder()
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_MINUTE)
                .leverage(1)
                .build();

        EmaCrossConfiguration emaCrossConfiguration = Mockito.mock(EmaCrossConfiguration.class);
        Mockito.when(emaCrossConfiguration.getAssetTradeWindow()).thenReturn(assetTradeWindow);

        Mockito.when(strategyConfigurationFactory.create("config1.json")).thenReturn(emaCrossConfiguration);

        // init()

        BinanceKlinesProvider klinesProvider = Mockito.mock(BinanceKlinesProvider.class);
        Mockito.when(binanceKlinesProviderFactory.create(Mockito.any())).thenReturn(klinesProvider);

        BarSeries barSeries = Mockito.mock(BarSeries.class);
        Mockito.when(klinesProvider.getSeries()).thenReturn(barSeries);

        ConditionalStrategy conditionalStrategy = Mockito.mock(ConditionalStrategy.class);
        Mockito.when(strategyFactory.create(emaCrossConfiguration, barSeries)).thenReturn(conditionalStrategy);

        // checkStrategy()

        BigDecimal entryPrice = BigDecimal.valueOf(78698);
        BigDecimal tp = BigDecimal.valueOf(80000);
        BigDecimal sl = BigDecimal.valueOf(78000);

        KlinesUpdateEvent klinesUpdateEvent = Mockito.mock(KlinesUpdateEvent.class);
        Mockito.when(klinesUpdateEvent.getSymbol()).thenReturn("BTCUSDT");
        Mockito.when(klinesUpdateEvent.getTimeframe()).thenReturn(Timeframe.ONE_MINUTE);
        Mockito.when(klinesUpdateEvent.getBarSeries()).thenReturn(barSeries);

        int endIndex = 1;
        Mockito.when(barSeries.getEndIndex()).thenReturn(endIndex);
        ZonedDateTime endDate = ZonedDateTime.now();
        Bar bar = Mockito.mock(Bar.class);
        Mockito.when(barSeries.getBar(endIndex)).thenReturn(bar);
        Mockito.when(bar.getEndTime()).thenReturn(endDate.toInstant());
        DecimalNum closePrice = DecimalNum.valueOf(entryPrice);
        Mockito.when(bar.getClosePrice()).thenReturn(closePrice);

        Mockito.when(conditionalStrategy.getConfiguration()).thenReturn(emaCrossConfiguration);
        Mockito.when(conditionalStrategy.isLongEntrySatisfied(endIndex)).thenReturn(true);
        Mockito.when(conditionalStrategy.getTp(Mockito.any(), Mockito.any())).thenReturn(tp);
        Mockito.when(conditionalStrategy.getSl(Mockito.any(), Mockito.any())).thenReturn(sl);

        // start
        strategyService.start();
        Thread.sleep(3000);
        strategyService.checkStrategy(klinesUpdateEvent);
        Thread.sleep(3000);
        System.out.println("Done!");
    }
}
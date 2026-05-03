package de.jansoh.rsistrategy.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.service.TelegramMessagingService;
import de.jansoh.rsistrategy.service.broker.binance.BinanceApiService;
import de.jansoh.rsistrategy.service.indicator.AtrIndicatorFactory;
import de.jansoh.rsistrategy.service.indicator.EMAIndicatorFactory;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProvider;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProviderFactory;
import de.jansoh.rsistrategy.service.kline.KlinesUpdateEvent;
import de.jansoh.rsistrategy.service.order.BinanceOrderEventProvider;
import de.jansoh.rsistrategy.service.order.OrderUpdateEventMapper;
import de.jansoh.rsistrategy.service.position.OpenPositionRegistry;
import de.jansoh.rsistrategy.service.position.PositionService;
import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategy;
import de.jansoh.rsistrategy.service.strategy.conditional.ConditionalStrategyFactory;
import de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy.EmaCrossConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.num.DecimalNum;

import java.time.ZonedDateTime;

@Import({
        StrategyService.class,
        BinanceApiService.class,
        PositionService.class,
        RestTemplate.class,
        ObjectMapper.class,
        OrderUpdateEventMapper.class,
        OpenPositionRegistry.class,
        BinanceOrderEventProvider.class
})
@ActiveProfiles("test")
@DataJpaTest
@DirtiesContext
@Disabled
class StrategyServiceDataIT {

    @MockitoBean
    BinanceKlinesProviderFactory binanceKlinesProviderFactory;

    @MockitoBean
    TelegramMessagingService telegramMessagingService;

    @MockitoBean
    ConditionalStrategyFactory strategyFactory;

    @MockitoBean
    AtrIndicatorFactory atrIndicatorFactory;

    @MockitoBean
    EMAIndicatorFactory emaIndicatorFactory;

    @Autowired
    StrategyService strategyService;

    @Test
    void checkStrategy_AndValidateRealPositions() {

        BinanceKlinesProvider se = Mockito.mock(BinanceKlinesProvider.class);
        Mockito.when(binanceKlinesProviderFactory.create(Mockito.any())).thenReturn(se);


        KlinesUpdateEvent klinesUpdateEvent = Mockito.mock(KlinesUpdateEvent.class);
        Mockito.when(klinesUpdateEvent.getSymbol()).thenReturn("BTCUSDT");
        Mockito.when(klinesUpdateEvent.getTimeframe()).thenReturn(Timeframe.ONE_MINUTE);

        BarSeries barSeries = Mockito.mock(BarSeries.class);
        Mockito.when(klinesUpdateEvent.getBarSeries()).thenReturn(barSeries);


        Strategy strategy = Mockito.mock(Strategy.class);
        ConditionalStrategy advancedStrategy = Mockito.mock(ConditionalStrategy.class);
        Mockito.when(strategyFactory.create(Mockito.any())).thenReturn(advancedStrategy);


        ATRIndicator atr = Mockito.mock(ATRIndicator.class);
        Mockito.when(atrIndicatorFactory.create(Mockito.any())).thenReturn(atr);


        double currentPrice1 = 71000;
        double currentPrice2 = 73000;
        double atrValue1 = 400;
        double atrValue2 = 500;

        Bar bar = Mockito.mock(Bar.class);
        Mockito.when(bar.getEndTime()).thenReturn(ZonedDateTime.now().minusDays(1).toInstant());
        Mockito.when(bar.getClosePrice())
                .thenReturn(DecimalNum.valueOf(currentPrice1))
                .thenReturn(DecimalNum.valueOf(currentPrice2)) // just avoid opening another position
                .thenReturn(DecimalNum.valueOf(currentPrice2 - 1000)); // just avoid opening another position
        Mockito.when(bar.getOpenPrice())
                .thenReturn(DecimalNum.valueOf(currentPrice1 - 1000.0))
                .thenReturn(DecimalNum.valueOf(currentPrice2 - 1000.0));

        Mockito.when(barSeries.getBar(Mockito.anyInt())).thenReturn(bar);
        Mockito.when(barSeries.getLastBar()).thenReturn(bar);


        EMAIndicator ema50 = Mockito.mock(EMAIndicator.class);
        Mockito.when(ema50.getValue(Mockito.anyInt()))
                .thenReturn(DecimalNum.valueOf(currentPrice1 - 500))
                .thenReturn(DecimalNum.valueOf(currentPrice2 - 500));

        Mockito.when(emaIndicatorFactory.createEMAIndicator(Mockito.any(), Mockito.anyInt())).thenReturn(ema50);

        Mockito.when(atr.getValue(Mockito.anyInt()))
                .thenReturn(DecimalNum.valueOf(atrValue1))
                .thenReturn(DecimalNum.valueOf(atrValue2));

        Mockito.when(strategy.shouldEnter(Mockito.anyInt()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        AssetTradeWindow tradeWindow = AssetTradeWindow.builder()
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_MINUTE)
                .build();


        EmaCrossConfiguration sc = Mockito.mock(EmaCrossConfiguration.class);
        strategyService.init(sc);

        // use breakpoints to debug each tick or add some sleep in between (because creating positions takes some time
        // and in reality, init() is called only once per minute).
        strategyService.checkStrategy(klinesUpdateEvent); // should open a long position
        // wait until TP or SL are triggered before executing the next tick
        strategyService.checkStrategy(klinesUpdateEvent);
        strategyService.checkStrategy(klinesUpdateEvent);

        System.out.println("Done!");
    }

    @Test
    void checkStrategy_AndValidate1RealPosition() {

        BinanceKlinesProvider se = Mockito.mock(BinanceKlinesProvider.class);
        Mockito.when(binanceKlinesProviderFactory.create(Mockito.any())).thenReturn(se);


        KlinesUpdateEvent klinesUpdateEvent = Mockito.mock(KlinesUpdateEvent.class);
        Mockito.when(klinesUpdateEvent.getSymbol()).thenReturn("BTCUSDT");
        Mockito.when(klinesUpdateEvent.getTimeframe()).thenReturn(Timeframe.ONE_MINUTE);

        BarSeries barSeries = Mockito.mock(BarSeries.class);
        Mockito.when(klinesUpdateEvent.getBarSeries()).thenReturn(barSeries);


        Strategy strategy = Mockito.mock(Strategy.class);
        ConditionalStrategy advancedStrategy = Mockito.mock(ConditionalStrategy.class);
        Mockito.when(strategyFactory.create(Mockito.any())).thenReturn(advancedStrategy);


        ATRIndicator atr = Mockito.mock(ATRIndicator.class);
        Mockito.when(atrIndicatorFactory.create(Mockito.any())).thenReturn(atr);


        double currentPrice1 = 71000;
        double currentPrice2 = 73000;
        double atrValue1 = 20;
        double atrValue2 = 500;

        Bar bar = Mockito.mock(Bar.class);
        Mockito.when(bar.getEndTime()).thenReturn(ZonedDateTime.now().minusDays(1).toInstant());
        Mockito.when(bar.getClosePrice())
                .thenReturn(DecimalNum.valueOf(currentPrice1))
                .thenReturn(DecimalNum.valueOf(currentPrice2)) // just avoid opening another position
                .thenReturn(DecimalNum.valueOf(currentPrice2 - 1000)); // just avoid opening another position
        Mockito.when(bar.getOpenPrice())
                .thenReturn(DecimalNum.valueOf(currentPrice1 - 1000.0))
                .thenReturn(DecimalNum.valueOf(currentPrice2 - 1000.0));

        Mockito.when(barSeries.getBar(Mockito.anyInt())).thenReturn(bar);
        Mockito.when(barSeries.getLastBar()).thenReturn(bar);


        EMAIndicator ema50 = Mockito.mock(EMAIndicator.class);
        Mockito.when(ema50.getValue(Mockito.anyInt()))
                .thenReturn(DecimalNum.valueOf(currentPrice1 - 500))
                .thenReturn(DecimalNum.valueOf(currentPrice2 - 500));

        Mockito.when(emaIndicatorFactory.createEMAIndicator(Mockito.any(), Mockito.anyInt())).thenReturn(ema50);

        Mockito.when(atr.getValue(Mockito.anyInt()))
                .thenReturn(DecimalNum.valueOf(atrValue1))
                .thenReturn(DecimalNum.valueOf(atrValue2));

        Mockito.when(strategy.shouldEnter(Mockito.anyInt()))
                .thenReturn(true)
                .thenReturn(false);

        AssetTradeWindow tradeWindow = AssetTradeWindow.builder()
                .symbol("BTCUSDT")
                .timeframe(Timeframe.ONE_MINUTE)
                .build();

        EmaCrossConfiguration sc = Mockito.mock(EmaCrossConfiguration.class);
        strategyService.init(sc);

        // use breakpoints to debug each tick or add some sleep in between (because creating positions takes some time
        // and in reality, init() is called only once per minute).
        strategyService.checkStrategy(klinesUpdateEvent); // should open a long position
        strategyService.checkStrategy(klinesUpdateEvent);

        System.out.println("Done!");
    }
}
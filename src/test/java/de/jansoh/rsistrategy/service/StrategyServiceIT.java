package de.jansoh.rsistrategy.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.num.DecimalNum;

import java.time.ZonedDateTime;

@SpringBootTest
class StrategyServiceIT {

    @MockitoBean
    TelegramMessagingService telegramMessagingService;
    @MockitoBean
    Strategy strategy;
    @MockitoBean
    ATRIndicator atr;
    @MockitoBean
    BarSeries barSeries;
    @MockitoBean
    EMAIndicatorFactory emaIndicatorFactory;

    @Autowired
    StrategyService strategyService;


    @Test
    void tick() {

        double currentPrice = 67000;
        Bar bar = Mockito.mock(Bar.class);
        Mockito.when(bar.getEndTime()).thenReturn(ZonedDateTime.now().minusDays(1));
        Mockito.when(bar.getClosePrice())
                .thenReturn(DecimalNum.valueOf(currentPrice))
                .thenReturn(DecimalNum.valueOf(currentPrice))
                .thenReturn(DecimalNum.valueOf(currentPrice));
        Mockito.when(bar.getOpenPrice())
                .thenReturn(DecimalNum.valueOf(currentPrice - 1000.0))
                .thenReturn(DecimalNum.valueOf(currentPrice - 1000.0))
                .thenReturn(DecimalNum.valueOf(currentPrice + 1000.0))
                .thenReturn(DecimalNum.valueOf(currentPrice + 1000.0));
        Mockito.when(barSeries.getBar(Mockito.anyInt())).thenReturn(bar);
        Mockito.when(barSeries.getLastBar()).thenReturn(bar);

        EMAIndicator ema50 = Mockito.mock(EMAIndicator.class);
        Mockito.when(ema50.getValue(Mockito.anyInt()))
                .thenReturn(DecimalNum.valueOf(currentPrice - 500))
                .thenReturn(DecimalNum.valueOf(currentPrice - 500))
                .thenReturn(DecimalNum.valueOf(currentPrice + 500))
                .thenReturn(DecimalNum.valueOf(currentPrice + 500));
        Mockito.when(emaIndicatorFactory.createEMAIndicator(Mockito.any(), Mockito.anyInt())).thenReturn(ema50);

        Mockito.when(atr.getValue(Mockito.anyInt()))
                .thenReturn(DecimalNum.valueOf(500.0))
                .thenReturn(DecimalNum.valueOf(2000.0)) // so we can see that TP/SL are being overwritten
                .thenReturn(DecimalNum.valueOf(500))
                .thenReturn(DecimalNum.valueOf(2000.0)); // so we can see that TP/SL are being overwritten

        Mockito.when(strategy.shouldEnter(Mockito.anyInt()))
                .thenReturn(true);

        // use breakpoints to debug each tick or add some sleep in between (because creating positions takes some time
        // and in reality, init() is called only once per minute).
        strategyService.tick(); // should open a long position
        strategyService.tick(); // could open a long position, but doesn't because there is already a position open. But TP and SL should now be overwritten.
        strategyService.tick(); // should open a short position
        strategyService.tick(); // could open a short position, but doesn't because there is already a position open. But TP and SL should now be overwritten.
    }

    @Test
    void tick_AndValidateRealPositions() throws InterruptedException {

        String symbol = "BTCUSDT";
        double originalQuantity = 0.01;

        double currentPrice = 69700;
        double atrValue = 10;

        Bar bar = Mockito.mock(Bar.class);
        Mockito.when(bar.getEndTime()).thenReturn(ZonedDateTime.now().minusDays(1));
        Mockito.when(bar.getClosePrice())
                .thenReturn(DecimalNum.valueOf(currentPrice))
                .thenReturn(DecimalNum.valueOf(currentPrice - 1000)); // just avoid opening another position
        Mockito.when(bar.getOpenPrice())
                .thenReturn(DecimalNum.valueOf(currentPrice - 1000.0));

        Mockito.when(barSeries.getBar(Mockito.anyInt())).thenReturn(bar);
        Mockito.when(barSeries.getLastBar()).thenReturn(bar);

        EMAIndicator ema50 = Mockito.mock(EMAIndicator.class);
        Mockito.when(ema50.getValue(Mockito.anyInt()))
                .thenReturn(DecimalNum.valueOf(currentPrice - 500));
        Mockito.when(emaIndicatorFactory.createEMAIndicator(Mockito.any(), Mockito.anyInt())).thenReturn(ema50);

        Mockito.when(atr.getValue(Mockito.anyInt()))
                .thenReturn(DecimalNum.valueOf(atrValue));

        Mockito.when(strategy.shouldEnter(Mockito.anyInt()))
                .thenReturn(true)
                .thenReturn(false);

        // use breakpoints to debug each tick or add some sleep in between (because creating positions takes some time
        // and in reality, init() is called only once per minute).
        strategyService.tick(); // should open a long position
        // wait until TP or SL are triggered before executing the next tick
        strategyService.tick(); // see if the closed position is correctly updated

        System.out.println("StrategyServiceIT.tick()");
    }


}
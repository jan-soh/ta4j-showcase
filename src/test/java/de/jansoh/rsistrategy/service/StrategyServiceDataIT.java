package de.jansoh.rsistrategy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jansoh.rsistrategy.model.OrderUpdateEventMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.num.DecimalNum;

import java.time.ZonedDateTime;

@Import({
        StrategyService.class,
        BinanceApiService.class,
        PositionService.class,
        BaseBarSeries.class,
        RestTemplate.class,
        BinanceWebSocketService.class,
        ObjectMapper.class,
        OrderUpdateEventMapper.class
})
@ActiveProfiles("test")
@DataJpaTest
class StrategyServiceDataIT {

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
    void tick_AndValidateRealPositions() {

        double currentPrice = 71841;
        double atrValue = 100;

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
                .thenReturn(true);
        //.thenReturn(false);

        // use breakpoints to debug each tick or add some sleep in between (because creating positions takes some time
        // and in reality, init() is called only once per minute).
        strategyService.tick(); // should open a long position
        // wait until TP or SL are triggered before executing the next tick
        strategyService.tick(); // see if the closed position is correctly updated

        System.out.println("StrategyServiceIT.tick()");
    }
}
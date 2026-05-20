package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.PositionSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FastEmaCrossingSlowEmaStrategyTest {

    private FastEmaCrossingSlowEmaStrategy strategy;
    private EmaCrossConfiguration configuration;
    private BarSeries barSeries;

    @BeforeEach
    void setUp() {
        barSeries = new BaseBarSeriesBuilder().withName("TestSeries").build();
        configuration = new EmaCrossConfiguration();
        configuration.setDefaults();
        strategy = new FastEmaCrossingSlowEmaStrategy(barSeries, configuration);
        ReflectionTestUtils.setField(strategy, "notionalMin", 50.0);
    }

    @Test
    void getSl_LongPosition_CalculatesCorrectly() {
        // Arrange
        configuration.setSlMultiplier(2.0);
        Position position = Position.builder()
                .side(PositionSide.LONG)
                .build();

        // height = abs(110 - 100) * 2.0 = 20.0
        // SL = 100 - 20.0 = 80.0
        Bar bar = createBar(100.0, 110.0);

        // Act
        BigDecimal sl = strategy.getSl(bar, position);

        // Assert
        assertEquals(0, new BigDecimal("80.0").compareTo(sl), "Expected 80.0 but got " + sl);
    }

    @Test
    void getSl_ShortPosition_CalculatesCorrectly() {
        // Arrange
        configuration.setSlMultiplier(1.5);
        Position position = Position.builder()
                .side(PositionSide.SHORT)
                .build();

        // height = abs(90 - 100) * 1.5 = 15.0
        // SL = 100 + 15.0 = 115.0
        Bar bar = createBar(100.0, 90.0);

        // Act
        BigDecimal sl = strategy.getSl(bar, position);

        // Assert
        assertEquals(0, new BigDecimal("115.0").compareTo(sl), "Expected 115.0 but got " + sl);
    }

    @Test
    void getTp_LongPosition_ReturnsDoubleEntryPrice() {
        // Arrange
        Position position = Position.builder()
                .side(PositionSide.LONG)
                .build();
        Bar bar = createBar(100.0, 105.0);

        // Act
        BigDecimal tp = strategy.getTp(bar, position);

        // Assert
        // 100.0 * 2.0 = 200.0
        assertEquals(0, new BigDecimal("200.0").compareTo(tp));
    }

    @Test
    void getTp_ShortPosition_BestPriceUsed() {
        // Arrange
        Position position = Position.builder()
                .side(PositionSide.SHORT)
                .quantity(BigDecimal.valueOf(10))
                .build();
        // Entry price 1000.0
        // bestPrice = 1000.0 * 0.1 = 100.0
        // minPrice = (50 / 10) * 1.05 = 5 * 1.05 = 5.25
        // bestPrice > minPrice -> 100.0
        Bar bar = createBar(1000.0, 950.0);

        // Act
        BigDecimal tp = strategy.getTp(bar, position);

        // Assert
        assertEquals(0, new BigDecimal("100.0").compareTo(tp));
    }

    @Test
    void getTp_ShortPosition_MinPriceUsed() {
        // Arrange
        Position position = Position.builder()
                .side(PositionSide.SHORT)
                .quantity(BigDecimal.valueOf(0.1))
                .build();
        // Entry price 10.0
        // bestPrice = 10.0 * 0.1 = 1.0
        // minPrice = (notional / vol) * 5% -> (50 / 0.1) * 1.05 = 525
        // bestPrice < minPrice -> 52.5
        Bar bar = createBar(100.0, 90.0);

        // Act
        BigDecimal tp = strategy.getTp(bar, position);

        // Assert
        assertEquals(0, new BigDecimal("525").compareTo(tp));
    }

    private Bar createBar(double open, double close) {
        ZonedDateTime now = ZonedDateTime.now();
        return new BaseBar(
                java.time.Duration.ofMinutes(1),
                now.toInstant(),
                now.plusMinutes(1).toInstant(),
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(Math.max(open, close)),
                DecimalNum.valueOf(Math.min(open, close)),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(1000),
                DecimalNum.valueOf(0),
                0
        );
    }
}

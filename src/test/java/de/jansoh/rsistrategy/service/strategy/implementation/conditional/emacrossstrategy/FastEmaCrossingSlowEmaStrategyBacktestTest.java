package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import de.jansoh.rsistrategy.model.AssetTradeWindow;
import de.jansoh.rsistrategy.model.Position;
import de.jansoh.rsistrategy.model.Timeframe;
import de.jansoh.rsistrategy.service.BinanceApiService;
import de.jansoh.rsistrategy.service.TelegramMessagingService;
import de.jansoh.rsistrategy.service.indicator.AtrIndicatorFactory;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProvider;
import de.jansoh.rsistrategy.service.kline.BinanceKlinesProviderFactory;
import de.jansoh.rsistrategy.service.kline.KlinesUpdateEventImpl;
import de.jansoh.rsistrategy.service.position.OpenPositionRegistry;
import de.jansoh.rsistrategy.service.position.PositionService;
import de.jansoh.rsistrategy.service.strategy.StrategyService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.DecimalNum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
class FastEmaCrossingSlowEmaStrategyBacktestTest {

    private static final String CSV_PATH = "src/test/resources/ohlcv-testdata/emacrossstrategy/FastEmaCrossingSlowEmaStrategyBacktestTest.csv";
    private static final String EXPECTED_OUTPUT_PATH = "src/test/resources/expected-output/emacrossstrategy/FastEmaCrossingSlowEmaStrategyBacktestTest.txt";

    private BinanceApiService binanceApiService;

    private PositionService positionService;

    private TelegramMessagingService telegramMessagingService;

    private EmaCrossConfigurationFactory strategyConfigurationFactory;

    private FastEmaCrossingSlowEmaStrategyFactory strategyFactory;

    private AtrIndicatorFactory atrIndicatorFactory;

    private OpenPositionRegistry openPositionRegistry;

    private BinanceKlinesProviderFactory binanceKlinesProviderFactory;

    private StrategyService strategyService;

    private final BarSeries series = new BaseBarSeriesBuilder().withName("BTCUSDT").build();

    private final List<Position> storedPositions = new ArrayList<>();

    @BeforeEach
    void setUp() {

        // Mock balance for calculateQuantity
        binanceApiService = Mockito.mock(BinanceApiService.class);
        when(binanceApiService.getBalance()).thenReturn(List.of(Map.of("asset", "USDT", "balance", "10000")));

        positionService = Mockito.mock(PositionService.class);
        // Mock PositionService to store positions in the list
        ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
        when(positionService.createPositionWithTpSl(positionCaptor.capture(), Mockito.eq(false))).thenAnswer(invocation -> {
            storedPositions.add(positionCaptor.getValue());
            return true;
        });

        telegramMessagingService = Mockito.mock(TelegramMessagingService.class);

        DecimalNum atrNum = DecimalNum.valueOf(25);
        ATRIndicator atr = Mockito.mock(ATRIndicator.class);
        when(atr.getValue(Mockito.anyInt())).thenReturn(atrNum);
        atrIndicatorFactory = Mockito.mock(AtrIndicatorFactory.class);
        when(atrIndicatorFactory.create(Mockito.any())).thenReturn(atr);

        binanceKlinesProviderFactory = Mockito.mock(BinanceKlinesProviderFactory.class);

        openPositionRegistry = Mockito.mock(OpenPositionRegistry.class);
        when(openPositionRegistry.getPositions(Mockito.any(AssetTradeWindow.class))).thenAnswer(invocation -> storedPositions.stream().filter(p -> !p.isClosed()).toList());
        when(openPositionRegistry.hasPositions(any(AssetTradeWindow.class))).thenReturn(true);


        BinanceKlinesProvider klinesProvider = Mockito.mock(BinanceKlinesProvider.class);
        when(klinesProvider.getSeries()).thenReturn(series);
        when(binanceKlinesProviderFactory.create(Mockito.any())).thenReturn(klinesProvider);

        strategyConfigurationFactory = new EmaCrossConfigurationFactory();
        strategyFactory = new FastEmaCrossingSlowEmaStrategyFactory();

        strategyService = new StrategyService(
                binanceApiService,
                positionService,
                telegramMessagingService,
                atrIndicatorFactory,
                openPositionRegistry,
                strategyConfigurationFactory,
                strategyFactory,
                binanceKlinesProviderFactory
        );

        ReflectionTestUtils.setField(strategyService, "strategiesToCreate", "EmaCrossConfiguration-test");
        ReflectionTestUtils.setField(strategyService, "sizePercentage", 5.0);
        ReflectionTestUtils.setField(strategyService, "commissionAsset", "USDT");
    }

    @Test
    void backtestFastEmaCrossingSlowEmaStrategy() throws Exception {

        strategyService.start();

        try (BufferedReader br = new BufferedReader(new FileReader(CSV_PATH))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                // time,open,high,low,close,Volume
                Instant startTime = Instant.ofEpochSecond(Long.parseLong(values[0]));
                series.addBar(new BaseBar(
                        null,
                        startTime,
                        startTime.plusSeconds(900),
                        series.numFactory().numOf(values[1]),
                        series.numFactory().numOf(values[2]),
                        series.numFactory().numOf(values[3]),
                        series.numFactory().numOf(values[4]),
                        series.numFactory().numOf(values[5]),
                        series.numFactory().numOf(0),
                        0
                ));

                strategyService.checkStrategy(KlinesUpdateEventImpl.builder()
                        .symbol("BTCUSDT")
                        .timeframe(Timeframe.FIFTEEN_MINUTES)
                        .barSeries(series)
                        .build());
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        StringBuilder sb = new StringBuilder();

        // Print results
        sb.append("side, openTime, averageOpenPrice, closedTime, averageClosedPrice, tpAlgoPrice, slAlgoPrice, realizedProfit").append("\n");
        for (Position p : storedPositions) {
            sb.append(String.format("%s, %s, %.2f, %s, %.2f, %.2f, %.2f, %s%n",
                    p.getSide(),
                    formatter.format(p.getOpenTime()),
                    p.getAverageOpenPrice(),
                    null != p.getClosedTime() ? formatter.format(p.getClosedTime()) : "",
                    p.getAverageClosedPrice(),
                    p.getTpAlgoPrice(),
                    p.getSlAlgoPrice(),
                    p.getRealizedProfit()));
        }

        String expectedResult = FileUtils.readFileToString(new File(EXPECTED_OUTPUT_PATH), StandardCharsets.UTF_8);

        String actual = sb.toString().replace("\r\n", "\n").replace("\r", "\n");
        String expected = expectedResult.replace("\r\n", "\n").replace("\r", "\n");

        Assertions.assertEquals(expected, actual);
    }
}
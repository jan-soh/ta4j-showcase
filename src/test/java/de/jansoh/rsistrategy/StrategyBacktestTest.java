package de.jansoh.rsistrategy;

import com.opencsv.exceptions.CsvValidationException;
import de.jansoh.rsistrategy.model.PositionReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class StrategyBacktestTest {

    @Test
    public void backtestEmaCrossStrategy() throws IOException, CsvValidationException {
        String csvPath = "src/test/resources/ohlcv-testdata/INDEX_BTCUSD, 240.csv";
        BarSeries series = loadBarSeriesFromCsv(csvPath);

        // Parameters
        int ema50Length = 50;
        int emaFilterLength = 200;
        int atrLength = 14;
        double tpMultiplier = 3.0;
        double slMultiplier = 2.0;

        // Optional Volume/Candle Filter (matching PineScript version)
        boolean useVolumeCandleFilter = false;
        int volumeStreakCount = 2;

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        OpenPriceIndicator openPrice = new OpenPriceIndicator(series);
        EMAIndicator ema50 = new EMAIndicator(closePrice, ema50Length);
        EMAIndicator emaFilter = new EMAIndicator(closePrice, emaFilterLength);
        ATRIndicator atr = new ATRIndicator(series, atrLength);
        VolumeIndicator volume = new VolumeIndicator(series);

        List<PositionReport> positions = new ArrayList<>();
        Trade currentTrade = null;
        double entryPriceVal = 0;
        double stopLoss = 0;
        double takeProfit = 0;

        for (int i = 1; i < series.getBarCount(); i++) {
            if (currentTrade == null) {
                // Entry Conditions
                boolean emaCrossAbove = openPrice.getValue(i).isLessThan(ema50.getValue(i)) &&
                        closePrice.getValue(i).isGreaterThan(ema50.getValue(i));
                boolean emaFilterLongMet = closePrice.getValue(i).isGreaterThan(emaFilter.getValue(i));

                boolean emaCrossBelow = openPrice.getValue(i).isGreaterThan(ema50.getValue(i)) &&
                        closePrice.getValue(i).isLessThan(ema50.getValue(i));
                boolean emaFilterShortMet = closePrice.getValue(i).isLessThan(emaFilter.getValue(i));

                // Volume/Candle Filters
                boolean volumeIncreased = isVolIncreasing(volume, i, volumeStreakCount);
                boolean lastCandleGreen = closePrice.getValue(i - 1).isGreaterThan(openPrice.getValue(i - 1));
                boolean lastCandleRed = closePrice.getValue(i - 1).isLessThan(openPrice.getValue(i - 1));

                boolean volCandleLongMet = !useVolumeCandleFilter || (volumeIncreased && lastCandleGreen);
                boolean volCandleShortMet = !useVolumeCandleFilter || (volumeIncreased && lastCandleRed);

                if (emaCrossAbove && emaFilterLongMet && volCandleLongMet) {
                    entryPriceVal = closePrice.getValue(i).doubleValue();
                    double atrVal = atr.getValue(i).doubleValue();
                    takeProfit = entryPriceVal + (tpMultiplier * atrVal);
                    stopLoss = entryPriceVal - (slMultiplier * atrVal);
                    currentTrade = Trade.buyAt(i, series.getBar(i).getClosePrice(), series.getBar(i).getClosePrice());
                } else if (emaCrossBelow && emaFilterShortMet && volCandleShortMet) {
                    entryPriceVal = closePrice.getValue(i).doubleValue();
                    double atrVal = atr.getValue(i).doubleValue();
                    takeProfit = entryPriceVal - (tpMultiplier * atrVal);
                    stopLoss = entryPriceVal + (slMultiplier * atrVal);
                    currentTrade = Trade.sellAt(i, series.getBar(i).getClosePrice(), series.getBar(i).getClosePrice());
                }
            } else {
                // Exit Conditions (TP/SL)
                double currentClose = closePrice.getValue(i).doubleValue();
                boolean exitTriggered = false;

                if (currentTrade.isBuy()) {
                    if (currentClose >= takeProfit || currentClose <= stopLoss) {
                        exitTriggered = true;
                    }
                } else {
                    if (currentClose <= takeProfit || currentClose >= stopLoss) {
                        exitTriggered = true;
                    }
                }

                if (exitTriggered) {
                    double pnl = currentTrade.isBuy() ? (currentClose - entryPriceVal) : (entryPriceVal - currentClose);
                    positions.add(PositionReport.builder()
                            .type(currentTrade.isBuy() ? "long" : "short")
                            .openDate(series.getBar(currentTrade.getIndex()).getEndTime().atZone(ZoneId.systemDefault()))
                            .entryPrice(entryPriceVal)
                            .closeDate(series.getBar(i).getEndTime().atZone(ZoneId.systemDefault()))
                            .exitPrice(currentClose)
                            .pnl(pnl)
                            .build());
                    currentTrade = null;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Position type,Open date,Entry price,Close date,Exit price,PnL").append("\r\n");
        positions.forEach(p -> sb.append(p).append("\r\n"));

        ClassPathResource resource = new ClassPathResource("expected-output/EmaCrossStrategy.csv");
        String expectedOutput = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
        Assertions.assertEquals(expectedOutput, sb.toString());

    }

    private boolean isVolIncreasing(VolumeIndicator volume, int index, int count) {
        if (count <= 0) return true;
        for (int i = 0; i < count; i++) {
            if (index - i - 1 < 0) return false;
            if (volume.getValue(index - i).doubleValue() <= volume.getValue(index - i - 1).doubleValue()) {
                return false;
            }
        }
        return true;
    }

    private BarSeries loadBarSeriesFromCsv(String filePath) {
        BarSeries series = new BaseBarSeriesBuilder().withName("BTCUSDT").build();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }
                String[] values = line.split(",");
                // time,open,high,low,close,Volume
                // 1773643500,73785.8,73800.1,73672.4,73680.7,767.562
                long timestamp = Long.parseLong(values[0]);
                Instant startTime = Instant.ofEpochSecond(timestamp);
                Instant endTime = startTime.plus(15, ChronoUnit.MINUTES);
                Num open = series.numFactory().numOf(Double.parseDouble(values[1]));
                Num high = series.numFactory().numOf(Double.parseDouble(values[2]));
                Num low = series.numFactory().numOf(Double.parseDouble(values[3]));
                Num close = series.numFactory().numOf(Double.parseDouble(values[4]));
                Num volume = series.numFactory().numOf(Double.parseDouble(values[5]));
                Bar bar = new BaseBar(null, startTime, endTime, open, high, low, close, volume, series.numFactory().numOf(0), 0);
                series.addBar(bar);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return series;
    }
}

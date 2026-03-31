package de.jansoh.rsistrategy;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import de.jansoh.rsistrategy.model.PositionReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class StrategyBacktestTest {

    @Test
    public void backtestEmaCrossStrategy() throws IOException, CsvValidationException {
        String csvPath = "src/test/resources/ohlcv-testdata/INDEX_BTCUSD, 240.csv";
        BarSeries series = loadBarSeries(csvPath);

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
                boolean lastCandleGreen = closePrice.getValue(i-1).isGreaterThan(openPrice.getValue(i-1));
                boolean lastCandleRed = closePrice.getValue(i-1).isLessThan(openPrice.getValue(i-1));

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
                            .openDate(series.getBar(currentTrade.getIndex()).getEndTime())
                            .entryPrice(entryPriceVal)
                            .closeDate(series.getBar(i).getEndTime())
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

    private BarSeries loadBarSeries(String filename) throws IOException, CsvValidationException {
        BarSeries series = new BaseBarSeries("BTCUSD");
        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            String[] header = reader.readNext(); // Skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                // time,open,high,low,close,Volume
                long timestamp = Long.parseLong(line[0]);
                ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                double open = Double.parseDouble(line[1]);
                double high = Double.parseDouble(line[2]);
                double low = Double.parseDouble(line[3]);
                double close = Double.parseDouble(line[4]);
                double vol = Double.parseDouble(line[5]);
                series.addBar(date, open, high, low, close, vol);
            }
        }
        return series;
    }
}

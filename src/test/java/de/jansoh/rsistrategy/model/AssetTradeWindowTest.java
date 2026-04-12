package de.jansoh.rsistrategy.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AssetTradeWindowTest {

    @Test
    void testEquals() {

        AssetTradeWindow a1 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(9).build();
        AssetTradeWindow a2 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(1).build();

        Assertions.assertEquals(a1, a2);
    }

    @Test
    void testNotEqualsDevSymbol() {

        AssetTradeWindow a1 = AssetTradeWindow.builder().symbol("DOGEUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(9).build();
        AssetTradeWindow a2 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(9).build();

        Assertions.assertNotEquals(a1, a2);
    }

    @Test
    void testNotEqualsDevFrame() {

        AssetTradeWindow a1 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.THREE_DAYS).leverage(9).build();
        AssetTradeWindow a2 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(9).build();

        Assertions.assertNotEquals(a1, a2);
    }

    @Test
    void testSameHashCode() {

        AssetTradeWindow a1 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(9).build();
        AssetTradeWindow a2 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(1).build();

        Assertions.assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void testNotSameHashCodeDevSymbol() {

        AssetTradeWindow a1 = AssetTradeWindow.builder().symbol("DOGEUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(9).build();
        AssetTradeWindow a2 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(9).build();

        Assertions.assertNotEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void testNotSameHashCodeDevFrame() {

        AssetTradeWindow a1 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.THREE_DAYS).leverage(9).build();
        AssetTradeWindow a2 = AssetTradeWindow.builder().symbol("BTCUSDT").timeframe(Timeframe.ONE_MINUTE).leverage(9).build();

        Assertions.assertNotEquals(a1.hashCode(), a2.hashCode());
    }
}
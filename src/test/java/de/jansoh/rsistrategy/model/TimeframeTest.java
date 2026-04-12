package de.jansoh.rsistrategy.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimeframeTest {

    @Test
    void fromShortcut_ShouldReturnCorrectEnum() {
        assertEquals(Timeframe.ONE_MINUTE, Timeframe.fromShortcut("1m"));
        assertEquals(Timeframe.FIVE_MINUTES, Timeframe.fromShortcut("5m"));
        assertEquals(Timeframe.ONE_HOUR, Timeframe.fromShortcut("1h"));
        assertEquals(Timeframe.ONE_DAY, Timeframe.fromShortcut("1d"));
        assertEquals(Timeframe.THREE_DAYS, Timeframe.fromShortcut("3d"));
        assertEquals(Timeframe.ONE_WEEK, Timeframe.fromShortcut("1w"));
    }

    @Test
    void fromShortcut_ShouldReturnNullForInvalidShortcut() {
        assertNull(Timeframe.fromShortcut("invalid"));
        assertNull(Timeframe.fromShortcut(""));
        assertNull(Timeframe.fromShortcut(null));
    }
}

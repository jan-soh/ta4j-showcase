package de.jansoh.rsistrategy.model;

public enum Timeframe {

    ONE_MINUTE("1m", 1),
    FIVE_MINUTES("5m", 5),
    FIFTEEN_MINUTES("15m", 15),
    THIRTY_MINUTES("30m", 30),
    ONE_HOUR("1h", 60),
    TWO_HOURS("2h", 120),
    FOUR_HOURS("4h", 240),
    SIX_HOURS("6h", 360),
    TWELVE_HOURS("12h", 720),
    ONE_DAY("1d", 1440),
    THREE_DAYS("3d", 4320),
    ONE_WEEK("1w", 10080),
    ;

    private final String shortcut;
    private final int minutes;

    Timeframe(String s, int minutes) {
        this.shortcut = s;
        this.minutes = minutes;
    }

    public String getShortcut() {
        return shortcut;
    }

    public static Timeframe fromShortcut(String shortcut) {
        for (Timeframe timeframe : values()) {
            if (timeframe.shortcut.equals(shortcut)) {
                return timeframe;
            }
        }
        return null;
    }

    public int getMinutes() {
        return minutes;
    }
}

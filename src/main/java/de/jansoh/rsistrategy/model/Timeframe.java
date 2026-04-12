package de.jansoh.rsistrategy.model;

public enum Timeframe {

    ONE_MINUTE("1m"),
    FIVE_MINUTES("5m"),
    FIFTEEN_MINUTES("15m"),
    THIRTY_MINUTES("30m"),
    ONE_HOUR("1h"),
    TWO_HOURS("2h"),
    FOUR_HOURS("4h"),
    SIX_HOURS("6h"),
    TWELVE_HOURS("12h"),
    ONE_DAY("1d"),
    THREE_DAYS("3d"),
    ONE_WEEK("1w"),
    ;

    private final String shortcut;

    Timeframe(String s) {
        this.shortcut = s;
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
}

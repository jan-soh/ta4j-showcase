package de.jansoh.rsistrategy.model;

/**
 * Represents predefined time intervals, each defined by a shortcut string
 * and its equivalent duration in minutes. This enumeration can be used
 * to handle standardized timeframes across various applications.
 */
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

    /**
     * The shortcut string representing the standardized textual identifier
     * for a specific timeframe. This value is used to easily distinguish
     * between predefined time intervals such as "1m" for one minute or "1h"
     * for one hour.
     */
    private final String shortcut;

    /**
     * The duration of the timeframe in minutes. This value indicates the
     * exact length of time that the predefined interval represents.
     */
    private final int minutes;

    /**
     * Constructs a Timeframe instance with the specified shortcut string and duration.
     *
     * @param s       the shortcut string representing the standardized identifier
     *                for this timeframe (e.g., "1m" for one minute, "1h" for one hour)
     * @param minutes the duration of the timeframe in minutes
     */
    Timeframe(String s, int minutes) {
        this.shortcut = s;
        this.minutes = minutes;
    }

    /**
     * Returns the shortcut string representing the standardized textual identifier
     * for a specific timeframe. The shortcut is used to distinguish between predefined
     * time intervals, such as "1m" for one minute or "1h" for one hour.
     *
     * @return the shortcut string associated with this timeframe
     */
    public String getShortcut() {
        return shortcut;
    }

    /**
     * Converts a shortcut string into its corresponding {@code Timeframe} enum instance.
     * Searches through all predefined timeframes and returns the matching timeframe based
     * on its shortcut string.
     *
     * @param shortcut the shortcut string representing the standardized textual identifier
     *                 for a specific timeframe (e.g., "1m" for one minute, "1h" for one hour)
     * @return the matching {@code Timeframe} instance corresponding to the provided shortcut,
     * or {@code null} if no matching timeframe is found
     */
    public static Timeframe fromShortcut(String shortcut) {
        for (Timeframe timeframe : values()) {
            if (timeframe.shortcut.equals(shortcut)) {
                return timeframe;
            }
        }
        return null;
    }

    /**
     * Returns the duration of the timeframe in minutes. This value indicates
     * the exact length of time that this predefined interval represents.
     *
     * @return the duration of the timeframe in minutes
     */
    public int getMinutes() {
        return minutes;
    }
}

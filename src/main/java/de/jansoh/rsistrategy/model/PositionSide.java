package de.jansoh.rsistrategy.model;

/**
 * Represents the side of a position in trading or investment contexts.
 * <p>
 * The PositionSide enum defines the orientation of a financial position,
 * indicating whether it is a long position, a short position, or both.
 * <p>
 * - LONG: Represents a position that anticipates the price of an asset to rise.
 * - SHORT: Represents a position that anticipates the price of an asset to fall.
 * - BOTH: Represents a combination of long and short positions.
 */
public enum PositionSide {
    LONG,
    SHORT,
    BOTH;
}

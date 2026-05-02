package de.jansoh.rsistrategy.model;

/**
 * Represents the state of a position in a system, typically used to track
 * lifecycle stages or the current condition of a position.
 * <p>
 * The possible states are:
 * <ul>
 * - NEW: Indicates that the position is newly created and has not been acted upon.
 * - OPEN: Indicates that the position is currently active or operational.
 * - CLOSED: Indicates that the position has been finalized or decommissioned.
 */
public enum PositionState {
    NEW,
    OPEN,
    CLOSED;
}

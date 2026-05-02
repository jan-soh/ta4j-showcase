package de.jansoh.rsistrategy.ta4jbridge.model;

import org.ta4j.core.analysis.cost.CostModel;
import org.ta4j.core.analysis.cost.ZeroCostModel;

/**
 * A specialized Position class representing a trading position. This class
 * extends the base functionality provided by the org.ta4j.core.Position
 * class and operates with TsTrade objects, which are custom trade
 * representations.
 * <p>
 * A trading position consists of an entry trade and optionally an exit trade.
 * TsPosition can be used to represent open or closed positions during trading
 * strategy evaluations.
 * <p>
 * Constructor Details:
 * - The first constructor initializes a TsPosition with an entry trade and
 * applies a zero-cost model for trading costs.
 * - The second constructor initializes a TsPosition with both an entry and
 * an exit trade.
 */
public class TaPosition extends org.ta4j.core.Position {

    public TaPosition(TaTrade entry) {
        CostModel costModel = new ZeroCostModel();
        super(entry, costModel, costModel);
    }
}

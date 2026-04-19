package de.jansoh.rsistrategy.ta4jbridge.model;

import org.ta4j.core.analysis.cost.CostModel;
import org.ta4j.core.analysis.cost.ZeroCostModel;

public class TsPosition extends org.ta4j.core.Position {

    public TsPosition(TsTrade entry) {
        CostModel costModel = new ZeroCostModel();
        super(entry, costModel, costModel);
    }

    public TsPosition(TsTrade entry, TsTrade exit) {
        super(entry, exit);
    }
}

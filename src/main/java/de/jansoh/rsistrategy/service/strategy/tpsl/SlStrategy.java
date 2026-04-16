package de.jansoh.rsistrategy.service.strategy.tpsl;

import de.jansoh.rsistrategy.model.Position;
import org.ta4j.core.Bar;

import java.math.BigDecimal;

public interface SlStrategy {

    BigDecimal getSl(Bar positionEntry, Position position);
}

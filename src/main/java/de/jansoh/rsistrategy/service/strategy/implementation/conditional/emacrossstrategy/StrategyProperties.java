package de.jansoh.rsistrategy.service.strategy.implementation.conditional.emacrossstrategy;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StrategyProperties {
    String value();
}

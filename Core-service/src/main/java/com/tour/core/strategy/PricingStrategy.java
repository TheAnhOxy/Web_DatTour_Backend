package com.tour.core.strategy;

import com.tour.core.entity.PricingRule;
import com.tour.core.entity.Departure;

import java.math.BigDecimal;

public interface PricingStrategy {

    String getRuleType();

    boolean isApplicable(PricingRule rule, Departure departure);

    BigDecimal apply(BigDecimal currentPrice, PricingRule rule);

    String describe(PricingRule rule);
}

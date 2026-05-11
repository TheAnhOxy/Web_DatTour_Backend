package com.tour.core.strategy.impl;

import com.tour.core.entity.Departure;
import com.tour.core.entity.PricingRule;
import com.tour.core.strategy.PricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class EarlyBirdPricingStrategy implements PricingStrategy {

    @Override
    public String getRuleType() {
        return "EARLY_BIRD";
    }

    @Override
    public boolean isApplicable(PricingRule rule, Departure departure) {
        if (!Boolean.TRUE.equals(rule.getIsActive())) {
            return false;
        }

        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), departure.getStartDate().toLocalDate());

        if (rule.getValidFrom() != null && LocalDate.now().isBefore(rule.getValidFrom().toLocalDate())) {
            return false;
        }
        if (rule.getValidTo() != null && LocalDate.now().isAfter(rule.getValidTo().toLocalDate())) {
            return false;
        }

        if (rule.getMinDaysBefore() != null && daysUntil < rule.getMinDaysBefore()) {
            return false;
        }
        if (rule.getMaxDaysBefore() != null && daysUntil > rule.getMaxDaysBefore()) {
            return false;
        }

        return true;
    }

    @Override
    public BigDecimal apply(BigDecimal currentPrice, PricingRule rule) {
        if ("PERCENT".equals(rule.getAdjustmentType())) {
            return currentPrice.multiply(
                    BigDecimal.ONE.subtract(
                            rule.getAdjustmentValue().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    )
            ).setScale(0, RoundingMode.HALF_UP);
        } else if ("FIXED".equals(rule.getAdjustmentType())) {
            return currentPrice.subtract(rule.getAdjustmentValue()).max(BigDecimal.ZERO);
        }
        return currentPrice;
    }

    @Override
    public String describe(PricingRule rule) {
        if ("PERCENT".equals(rule.getAdjustmentType())) {
            return "Early Bird -" + rule.getAdjustmentValue() + "%";
        }
        return "Early Bird -" + rule.getAdjustmentValue() + " VNĐ";
    }
}

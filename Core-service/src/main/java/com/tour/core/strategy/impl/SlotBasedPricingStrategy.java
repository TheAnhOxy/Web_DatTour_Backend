package com.tour.core.strategy.impl;

import com.tour.core.entity.Departure;
import com.tour.core.entity.PricingRule;
import com.tour.core.strategy.PricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SlotBasedPricingStrategy implements PricingStrategy {

    @Override
    public String getRuleType() {
        return "SLOT_BASED";
    }

    @Override
    public boolean isApplicable(PricingRule rule, Departure departure) {
        if (!Boolean.TRUE.equals(rule.getIsActive())) {
            return false;
        }

        int slotsLeft = departure.getMaxSlots() - departure.getBookedSlots();

        if (rule.getMinSlotsLeft() != null && slotsLeft < rule.getMinSlotsLeft()) {
            return false;
        }
        if (rule.getMaxSlotsLeft() != null && slotsLeft > rule.getMaxSlotsLeft()) {
            return false;
        }

        return true;
    }

    @Override
    public BigDecimal apply(BigDecimal currentPrice, PricingRule rule) {
        if ("PERCENT".equals(rule.getAdjustmentType())) {
            return currentPrice.multiply(
                    BigDecimal.ONE.add(
                            rule.getAdjustmentValue().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    )
            ).setScale(0, RoundingMode.HALF_UP);
        } else if ("FIXED".equals(rule.getAdjustmentType())) {
            return currentPrice.add(rule.getAdjustmentValue());
        }
        return currentPrice;
    }

    @Override
    public String describe(PricingRule rule) {
        if ("PERCENT".equals(rule.getAdjustmentType())) {
            return "Giá cao điểm (còn ít chỗ) +" + rule.getAdjustmentValue() + "%";
        }
        return "Giá cao điểm (còn ít chỗ) +" + rule.getAdjustmentValue() + " VNĐ";
    }
}

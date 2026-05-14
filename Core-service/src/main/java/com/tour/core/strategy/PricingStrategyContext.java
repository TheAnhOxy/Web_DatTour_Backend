package com.tour.core.strategy;

import com.tour.core.entity.Departure;
import com.tour.core.entity.PricingRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingStrategyContext {

    private final List<PricingStrategy> strategies;
    private Map<String, PricingStrategy> strategyMap;

    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(PricingStrategy::getRuleType, s -> s));
        log.info("Loaded {} pricing strategies: {}", strategyMap.size(), strategyMap.keySet());
    }

    public BigDecimal applyRules(
            BigDecimal basePrice,
            Departure departure,
            List<PricingRule> rules,
            List<String> appliedDescriptions) {

        if (strategyMap == null) {
            init();
        }

        List<PricingRule> sorted = rules.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .sorted(Comparator.comparing(PricingRule::getPriority))
                .toList();

        BigDecimal price = basePrice;
        for (PricingRule rule : sorted) {
            PricingStrategy strategy = strategyMap.get(rule.getRuleType());
            if (strategy == null) {
                log.warn("Strategy không tìm thấy cho ruleType: {}", rule.getRuleType());
                continue;
            }
            if (strategy.isApplicable(rule, departure)) {
                BigDecimal newPrice = strategy.apply(price, rule);
                appliedDescriptions.add(strategy.describe(rule));
                log.info("Applied {} rule: {} → {}", rule.getRuleType(), price, newPrice);
                price = newPrice;
            }
        }

        return price.max(BigDecimal.ZERO);
    }
}

package com.tour.core.repository;

import com.tour.core.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    List<PricingRule> findByDepartureIdOrderByPriorityAsc(Long departureId);

    List<PricingRule> findByDepartureIdAndIsActiveTrueOrderByPriorityAsc(Long departureId);
}

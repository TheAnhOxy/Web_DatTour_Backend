package com.tour.core.service;

import com.tour.core.dto.request.PricingRuleRequest;
import com.tour.core.dto.response.PricingRuleResponse;
import com.tour.core.dto.response.PriceCalculateResponse;

import java.util.List;

public interface PricingRuleService {

    List<PricingRuleResponse> getByDepartureId(Long departureId);

    PricingRuleResponse create(Long departureId, PricingRuleRequest request);

    PricingRuleResponse update(Long ruleId, PricingRuleRequest request);

    void delete(Long ruleId);

    PricingRuleResponse toggleActive(Long ruleId);

    PriceCalculateResponse calculatePrice(Long departureId, int adultCount, int child1014Count,
                                          int child49Count, int babyCount);
}

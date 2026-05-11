package com.tour.core.service.impl;

import com.tour.core.dto.request.PricingRuleRequest;
import com.tour.core.dto.response.PriceBreakdownItem;
import com.tour.core.dto.response.PriceCalculateResponse;
import com.tour.core.dto.response.PricingRuleResponse;
import com.tour.core.entity.Departure;
import com.tour.core.entity.PriceConfig;
import com.tour.core.entity.PricingRule;
import com.tour.core.exception.InvalidDataException;
import com.tour.core.exception.ResourceNotFoundException;
import com.tour.core.repository.DepartureRepository;
import com.tour.core.repository.PriceConfigRepository;
import com.tour.core.repository.PricingRuleRepository;
import com.tour.core.service.PricingRuleService;
import com.tour.core.strategy.PricingStrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingRuleServiceImpl implements PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final DepartureRepository departureRepository;
    private final PriceConfigRepository priceConfigRepository;
    private final PricingStrategyContext pricingStrategyContext;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getByDepartureId(Long departureId) {
        if (!departureRepository.existsById(departureId)) {
            throw new ResourceNotFoundException("Departure không tồn tại: " + departureId);
        }
        return pricingRuleRepository.findByDepartureIdOrderByPriorityAsc(departureId)
                .stream()
                .map(rule -> {
                    PricingRuleResponse response = modelMapper.map(rule, PricingRuleResponse.class);
                    response.setDepartureId(departureId);
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional
    public PricingRuleResponse create(Long departureId, PricingRuleRequest request) {
        Departure departure = findDeparture(departureId);
        validateRuleType(request.getRuleType());
        validateAdjustmentType(request.getAdjustmentType());

        PricingRule rule = modelMapper.map(request, PricingRule.class);
        rule.setDeparture(departure);
        PricingRule saved = pricingRuleRepository.save(rule);
        log.info("Tạo pricing rule {} cho departure {}", saved.getId(), departureId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PricingRuleResponse update(Long ruleId, PricingRuleRequest request) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule không tồn tại: " + ruleId));

        validateRuleType(request.getRuleType());
        validateAdjustmentType(request.getAdjustmentType());

        modelMapper.map(request, rule);
        PricingRule saved = pricingRuleRepository.save(rule);
        log.info("Cập nhật pricing rule {}", ruleId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long ruleId) {
        PricingRule rule = findRule(ruleId);
        pricingRuleRepository.delete(rule);
        log.info("Xóa pricing rule {}", ruleId);
    }

    @Override
    @Transactional
    public PricingRuleResponse toggleActive(Long ruleId) {
        PricingRule rule = findRule(ruleId);
        rule.setIsActive(!Boolean.TRUE.equals(rule.getIsActive()));
        PricingRule saved = pricingRuleRepository.save(rule);
        log.info("Toggle rule {} → isActive={}", ruleId, saved.getIsActive());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceCalculateResponse calculatePrice(Long departureId, int adultCount, int child1014Count,
                                                  int child49Count, int babyCount) {
        Departure departure = findDeparture(departureId);

        if (!"OPEN".equals(departure.getStatus())) {
            throw new InvalidDataException("Departure này đã đóng");
        }

        int totalPassengers = adultCount + child1014Count + child49Count + babyCount;
        int availableSlots = departure.getMaxSlots() - departure.getBookedSlots();

        if (totalPassengers > availableSlots) {
            throw new InvalidDataException("Không đủ chỗ. Còn " + availableSlots + " chỗ");
        }

        PriceConfig config = priceConfigRepository.findByDepartureId(departureId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cấu hình giá"));

        List<PricingRule> rules = pricingRuleRepository
                .findByDepartureIdAndIsActiveTrueOrderByPriorityAsc(departureId);

        List<String> appliedRules = new ArrayList<>();
        List<PriceBreakdownItem> breakdown = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        PriceBreakdownItem adult = calcItem("ADULT", adultCount, config.getAdultPrice(), rules, departure, appliedRules);
        PriceBreakdownItem c1014 = calcItem("CHILD_10_14", child1014Count, config.getChild1014Price(), rules, departure, appliedRules);
        PriceBreakdownItem c49 = calcItem("CHILD_4_9", child49Count, config.getChild49Price(), rules, departure, appliedRules);
        PriceBreakdownItem baby = calcItem("BABY", babyCount, config.getBabyPrice(), rules, departure, appliedRules);

        for (PriceBreakdownItem item : List.of(adult, c1014, c49, baby)) {
            if (item != null) {
                breakdown.add(item);
                subtotal = subtotal.add(item.getBaseUnitPrice().multiply(BigDecimal.valueOf(item.getCount())));
                totalAmount = totalAmount.add(item.getTotal());
            }
        }

        BigDecimal totalDiscount = subtotal.subtract(totalAmount).max(BigDecimal.ZERO);

        log.info("Tính giá departure={} total={} applied={}", departureId, totalAmount, appliedRules);

        return PriceCalculateResponse.builder()
                .departureId(departureId)
                .tourTitle(departure.getTour().getTitle())
                .departureDate(departure.getStartDate())
                .appliedRules(appliedRules.stream().distinct().toList())
                .breakdown(breakdown)
                .subtotal(subtotal)
                .totalDiscount(totalDiscount)
                .totalAmount(totalAmount)
                .totalPassengers(totalPassengers)
                .availableSlots(availableSlots)
                .build();
    }

    private PriceBreakdownItem calcItem(String type, int count, BigDecimal basePrice,
                                        List<PricingRule> rules, Departure departure,
                                        List<String> appliedRules) {
        if (count == 0) {
            return null;
        }
        List<String> desc = new ArrayList<>();
        BigDecimal finalPrice = pricingStrategyContext.applyRules(basePrice, departure, rules, desc);
        appliedRules.addAll(desc);
        return PriceBreakdownItem.builder()
                .passengerType(type)
                .count(count)
                .baseUnitPrice(basePrice)
                .finalUnitPrice(finalPrice)
                .discountAmount(basePrice.subtract(finalPrice).max(BigDecimal.ZERO))
                .total(finalPrice.multiply(BigDecimal.valueOf(count)))
                .build();
    }

    private Departure findDeparture(Long departureId) {
        return departureRepository.findById(departureId)
                .orElseThrow(() -> new ResourceNotFoundException("Departure không tồn tại: " + departureId));
    }

    private PricingRule findRule(Long ruleId) {
        return pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule không tồn tại: " + ruleId));
    }

    private PricingRuleResponse toResponse(PricingRule rule) {
        PricingRuleResponse response = modelMapper.map(rule, PricingRuleResponse.class);
        response.setDepartureId(rule.getDeparture().getId());
        return response;
    }

    private void validateRuleType(String ruleType) {
        if (!List.of("EARLY_BIRD", "LAST_MINUTE", "SLOT_BASED").contains(ruleType)) {
            throw new InvalidDataException("Loại rule không hợp lệ: " + ruleType);
        }
    }

    private void validateAdjustmentType(String adjustmentType) {
        if (!List.of("PERCENT", "FIXED").contains(adjustmentType)) {
            throw new InvalidDataException("Loại điều chỉnh không hợp lệ: " + adjustmentType);
        }
    }
}

package com.tour.core.controller;

import com.tour.core.dto.request.PricingRuleRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.PriceCalculateResponse;
import com.tour.core.dto.response.PricingRuleResponse;
import com.tour.core.service.PricingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pricing Rule", description = "Quản lý rule giảm giá và tính giá")
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @GetMapping("/core/departures/{departureId}/pricing-rules")
    @Operation(summary = "Lấy danh sách pricing rules của departure")
    public ResponseEntity<ApiResponse> getByDepartureId(@PathVariable Long departureId) {
        List<PricingRuleResponse> rules = pricingRuleService.getByDepartureId(departureId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách pricing rules thành công")
                .data(rules)
                .build());
    }

    @PostMapping("/core/departures/{departureId}/pricing-rules")
    @Operation(summary = "Tạo pricing rule mới")
    public ResponseEntity<ApiResponse> create(
            @PathVariable Long departureId,
            @Valid @RequestBody PricingRuleRequest request) {
        PricingRuleResponse rule = pricingRuleService.create(departureId, request);
        return ResponseEntity.status(201).body(ApiResponse.builder()
                .status(201)
                .message("Tạo pricing rule thành công")
                .data(rule)
                .build());
    }

    @PutMapping("/core/pricing-rules/{ruleId}")
    @Operation(summary = "Cập nhật pricing rule")
    public ResponseEntity<ApiResponse> update(
            @PathVariable Long ruleId,
            @Valid @RequestBody PricingRuleRequest request) {
        PricingRuleResponse rule = pricingRuleService.update(ruleId, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật pricing rule thành công")
                .data(rule)
                .build());
    }

    @DeleteMapping("/core/pricing-rules/{ruleId}")
    @Operation(summary = "Xóa pricing rule")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long ruleId) {
        pricingRuleService.delete(ruleId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa pricing rule thành công")
                .data(null)
                .build());
    }

    @PatchMapping("/core/pricing-rules/{ruleId}/toggle")
    @Operation(summary = "Bật/tắt pricing rule")
    public ResponseEntity<ApiResponse> toggleActive(@PathVariable Long ruleId) {
        PricingRuleResponse rule = pricingRuleService.toggleActive(ruleId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật trạng thái rule thành công")
                .data(rule)
                .build());
    }

    @GetMapping("/core/departures/{departureId}/calculate-price")
    @Operation(summary = "Tính giá thực tế theo Strategy Pattern (EARLY_BIRD/LAST_MINUTE/SLOT_BASED)")
    public ResponseEntity<ApiResponse> calculatePrice(
            @PathVariable Long departureId,
            @RequestParam(defaultValue = "1") int adultCount,
            @RequestParam(defaultValue = "0") int child1014Count,
            @RequestParam(defaultValue = "0") int child49Count,
            @RequestParam(defaultValue = "0") int babyCount) {
        PriceCalculateResponse response = pricingRuleService.calculatePrice(
                departureId, adultCount, child1014Count, child49Count, babyCount);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Tính giá thành công")
                .data(response)
                .build());
    }
}

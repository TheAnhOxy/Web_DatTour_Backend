package com.tour.core.controller;

import com.tour.core.dto.request.PriceConfigRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.PriceConfigResponse;
import com.tour.core.service.PriceConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/core/departures/{departureId}/price-config")
@Tag(name = "Price Config", description = "Quản lý cấu hình giá cho lịch khởi hành")
@RequiredArgsConstructor
@Validated
public class PriceConfigController {

    private final PriceConfigService priceConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse> get(
            @PathVariable("departureId") @Positive(message = "id phải lớn hơn 0") Long departureId) {
        PriceConfigResponse resp = priceConfigService.getByDepartureId(departureId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy thông tin cấu hình giá thành công")
                .data(resp)
                .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse> upsert(
            @PathVariable("departureId") @Positive(message = "id phải lớn hơn 0") Long departureId,
            @Valid @RequestBody PriceConfigRequest request) {
        PriceConfigResponse resp = priceConfigService.upsert(departureId, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật cấu hình giá thành công")
                .data(resp)
                .build());
    }
}

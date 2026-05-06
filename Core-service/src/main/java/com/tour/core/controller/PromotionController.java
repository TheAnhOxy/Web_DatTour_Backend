package com.tour.core.controller;

import com.tour.core.dto.request.PromotionRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.PromotionResponse;
import com.tour.core.dto.response.PromotionValidateResponse;
import com.tour.core.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
@Tag(name = "Promotion", description = "Quản lý mã khuyến mãi")
@RequiredArgsConstructor
@Validated
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(summary = "Lấy danh sách promotion")
    public ResponseEntity<ApiResponse> getAll(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        boolean elevated = hasElevatedRole();
        Page<PromotionResponse> promotions = elevated
            ? promotionService.getStaffList(page, size)
            : promotionService.getCustomerList(isActive, page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách promotion thành công")
                .data(promotions)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy promotion theo id")
    public ResponseEntity<ApiResponse> getById(@PathVariable @Positive Long id) {
        PromotionResponse response = promotionService.getById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy promotion thành công")
                .data(response)
                .build());
    }

    @GetMapping("/validate")
    @Operation(summary = "Kiểm tra mã promotion")
    public ResponseEntity<ApiResponse> validate(@RequestParam @NotBlank String code) {
        PromotionValidateResponse response = promotionService.validate(code);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Kiểm tra promotion thành công")
                .data(response)
                .build());
    }

    @PostMapping
    @Operation(summary = "Tạo promotion")
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody PromotionRequest request) {
        PromotionResponse response = promotionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.builder()
                .status(201)
                .message("Tạo promotion thành công")
                .data(response)
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật promotion")
    public ResponseEntity<ApiResponse> update(@PathVariable @Positive Long id,
            @Valid @RequestBody PromotionRequest request) {
        PromotionResponse response = promotionService.update(id, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật promotion thành công")
                .data(response)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa promotion")
    public ResponseEntity<ApiResponse> delete(@PathVariable @Positive Long id) {
        promotionService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa promotion thành công")
                .data(null)
                .build());
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Bật/tắt promotion")
    public ResponseEntity<ApiResponse> toggle(@PathVariable @Positive Long id) {
        PromotionResponse response = promotionService.toggle(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Đổi trạng thái promotion thành công")
                .data(response)
                .build());
    }

    private boolean hasElevatedRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_STAFF".equals(a.getAuthority()));
    }
}

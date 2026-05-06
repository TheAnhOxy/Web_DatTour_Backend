package com.tour.core.controller;

import com.tour.core.dto.request.TourRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.TourListResponse;
import com.tour.core.dto.response.TourDetailResponse;
import com.tour.core.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tours")
@Tag(name = "Tour", description = "Quản lý tour")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    @GetMapping
    @Operation(summary = "Get active tours for customer")
    public ResponseEntity<ApiResponse> getAllForCustomer(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "isHot", required = false) Boolean isHot,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<TourListResponse> tours = tourService.getAllForCustomer(categoryId, isHot, page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách tour hoạt động thành công")
                .data(tours)
                .build());
    }

    @GetMapping("/admin")
    @Operation(summary = "Get all tours for admin with filters")
    public ResponseEntity<ApiResponse> getAllForAdmin(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "isHot", required = false) Boolean isHot,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<TourListResponse> tours = tourService.getAllForAdmin(status, categoryId, isHot, page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách tour cho admin thành công")
                .data(tours)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tour by id")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        TourDetailResponse tour = tourService.getById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy chi tiết tour thành công")
                .data(tour)
                .build());
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get tour by slug")
    public ResponseEntity<ApiResponse> getBySlug(@PathVariable String slug) {
        TourDetailResponse tour = tourService.getBySlug(slug);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy chi tiết tour theo slug thành công")
                .data(tour)
                .build());
    }

    @PostMapping
    @Operation(summary = "Create tour")
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody TourRequest request) {
        TourDetailResponse tour = tourService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.builder()
                .status(201)
                .message("Tạo tour thành công")
                .data(tour)
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tour")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @Valid @RequestBody TourRequest request) {
        TourDetailResponse tour = tourService.update(id, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật tour thành công")
                .data(tour)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tour")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        tourService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa tour thành công")
                .data(null)
                .build());
    }

    @PatchMapping("/{id}/hot")
    @Operation(summary = "Toggle hot status")
    public ResponseEntity<ApiResponse> toggleHot(@PathVariable Long id) {
        TourDetailResponse tour = tourService.toggleHot(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Đổi trạng thái hot thành công")
                .data(tour)
                .build());
    }
}

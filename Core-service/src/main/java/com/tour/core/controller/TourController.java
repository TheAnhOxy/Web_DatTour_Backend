package com.tour.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.core.dto.request.TourRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.TourListResponse;
import com.tour.core.dto.response.TourDetailResponse;
import com.tour.core.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/core/tours")
@Tag(name = "Tour", description = "Quản lý tour")
@RequiredArgsConstructor
@Slf4j
public class TourController {

    private final TourService tourService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "Get active tours for customer")
    public ResponseEntity<ApiResponse> getAllForCustomer(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "isHot", required = false) Boolean isHot,
            @RequestParam(value = "destinationId", required = false) Long destinationId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<TourListResponse> tours = tourService.getAllForCustomer(categoryId, isHot, destinationId, page, size);
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
            @RequestParam(value = "destinationId", required = false) Long destinationId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<TourListResponse> tours = tourService.getAllForAdmin(status, categoryId, isHot, destinationId, page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách tour cho admin thành công")
                .data(tours)
                .build());
    }

    @GetMapping("/admin/search")
    @Operation(summary = "Search tours for admin with keyword and filters")
    public ResponseEntity<ApiResponse> searchForAdmin(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "isHot", required = false) Boolean isHot,
            @RequestParam(value = "destinationId", required = false) Long destinationId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<TourListResponse> tours = tourService.searchForAdmin(keyword, status, categoryId, isHot, destinationId, page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Tìm kiếm tour thành công")
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create tour with optional images (multipart)")
    public ResponseEntity<ApiResponse> create(
            @RequestPart("tour") String tourJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            TourRequest request = objectMapper.readValue(tourJson, TourRequest.class);
            TourDetailResponse tour = tourService.create(request, images);
            return ResponseEntity.status(201).body(ApiResponse.builder()
                    .status(201)
                    .message("Tạo tour thành công")
                    .data(tour)
                    .build());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Không thể parse dữ liệu tour JSON: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .status(400)
                    .message("Dữ liệu tour không hợp lệ: " + e.getMessage())
                    .data(null)
                    .build());
        }
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

package com.tour.core.controller;

import com.tour.core.dto.request.TourImageReorderRequest;
import com.tour.core.dto.request.TourImageRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.TourImageResponse;
import com.tour.core.service.TourImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core/tours/{tourId}/images")
@Tag(name = "Tour Image", description = "Quản lý ảnh tour")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TourImageController {

    private final TourImageService tourImageService;

    @GetMapping
    @Operation(summary = "Lấy danh sách ảnh của tour theo sortOrder")
    public ResponseEntity<ApiResponse> getByTourId(@PathVariable Long tourId) {
        List<TourImageResponse> images = tourImageService.getByTourId(tourId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách ảnh thành công")
                .data(images)
                .build());
    }

    @PostMapping
    @Operation(summary = "Thêm 1 ảnh vào tour")
    public ResponseEntity<ApiResponse> addImage(
            @PathVariable Long tourId,
            @Valid @RequestBody TourImageRequest request) {
        TourImageResponse image = tourImageService.addImage(tourId, request);
        return ResponseEntity.status(201).body(ApiResponse.builder()
                .status(201)
                .message("Thêm ảnh thành công")
                .data(image)
                .build());
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Xóa 1 ảnh khỏi tour")
    public ResponseEntity<ApiResponse> deleteImage(
            @PathVariable Long tourId,
            @PathVariable Long imageId) {
        tourImageService.deleteImage(tourId, imageId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa ảnh thành công")
                .data(null)
                .build());
    }

    @PatchMapping("/{imageId}/cover")
    @Operation(summary = "Đặt ảnh làm ảnh bìa, reset các ảnh khác")
    public ResponseEntity<ApiResponse> setCover(
            @PathVariable Long tourId,
            @PathVariable Long imageId) {
        TourImageResponse image = tourImageService.setCover(tourId, imageId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Đặt ảnh bìa thành công")
                .data(image)
                .build());
    }

    @PutMapping("/reorder")
    @Operation(summary = "Sắp xếp lại thứ tự ảnh")
    public ResponseEntity<ApiResponse> reorder(
            @PathVariable Long tourId,
            @Valid @RequestBody TourImageReorderRequest request) {
        List<TourImageResponse> images = tourImageService.reorder(tourId, request.getImageIds());
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Sắp xếp ảnh thành công")
                .data(images)
                .build());
    }
}

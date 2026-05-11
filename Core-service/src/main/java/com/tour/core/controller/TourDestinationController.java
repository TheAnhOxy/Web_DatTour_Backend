package com.tour.core.controller;

import com.tour.core.dto.request.TourDestinationRequest;
import com.tour.core.dto.request.TourDestinationsRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.TourDetailResponse;
import com.tour.core.service.TourDestinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core/tours/{tourId}/destinations")
@Tag(name = "Tour Destination", description = "Quản lý điểm đến của tour")
@RequiredArgsConstructor
@Slf4j
public class TourDestinationController {

    private final TourDestinationService tourDestinationService;

    @PostMapping("/add")
    @Operation(summary = "Thêm 1 điểm đến vào tour")
    public ResponseEntity<ApiResponse> addDestination(
            @PathVariable Long tourId,
            @Valid @RequestBody TourDestinationRequest request) {
        TourDetailResponse tour = tourDestinationService.addDestination(tourId, request.getDestinationId());
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Thêm điểm đến vào tour thành công")
                .data(tour)
                .build());
    }

    @DeleteMapping("/{destinationId}")
    @Operation(summary = "Xóa 1 điểm đến khỏi tour")
    public ResponseEntity<ApiResponse> removeDestination(
            @PathVariable Long tourId,
            @PathVariable Long destinationId) {
        TourDetailResponse tour = tourDestinationService.removeDestination(tourId, destinationId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa điểm đến khỏi tour thành công")
                .data(tour)
                .build());
    }

    @PutMapping
    @Operation(summary = "Gán lại toàn bộ destinations cho tour (truyền rỗng để xóa hết)")
    public ResponseEntity<ApiResponse> setDestinations(
            @PathVariable Long tourId,
            @Valid @RequestBody TourDestinationsRequest request) {
        TourDetailResponse tour = tourDestinationService.setDestinations(tourId, request.getDestinationIds());
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật danh sách điểm đến thành công")
                .data(tour)
                .build());
    }
}

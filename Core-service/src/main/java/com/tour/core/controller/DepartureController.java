package com.tour.core.controller;

import com.tour.core.dto.request.DepartureRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.DepartureResponse;
import com.tour.core.service.DepartureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departures")
@Tag(name = "Departure", description = "Quản lý lịch khởi hành của tour")
@RequiredArgsConstructor
@Validated
public class DepartureController {

    private final DepartureService departureService;

    @GetMapping
    @Operation(summary = "Get departures for a tour (paged)")
    public ResponseEntity<ApiResponse> list(
            @RequestParam @Positive(message = "tourId phải lớn hơn 0") Long tourId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DepartureResponse> deps = departureService.getByTourId(tourId, status, page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách lịch khởi hành thành công")
                .data(deps)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get departure by id")
    public ResponseEntity<ApiResponse> getById(@PathVariable @Positive(message = "id phải lớn hơn 0") Long id) {
        DepartureResponse resp = departureService.getById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy thông tin lịch khởi hành thành công")
                .data(resp)
                .build());
    }

    @PostMapping
    @Operation(summary = "Create departure")
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody DepartureRequest request) {
        DepartureResponse resp = departureService.create(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.builder()
                .status(201)
                .message("Tạo departure thành công")
                .data(resp)
                .build());
    }

    @GetMapping("/available")
    @Operation(summary = "Get available (OPEN) departures for customers")
    public ResponseEntity<ApiResponse> listAvailable(
            @RequestParam @Positive(message = "tourId phải lớn hơn 0") Long tourId) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy lịch khởi hành khả dụng")
                .data(departureService.getOpenByTourId(tourId))
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update departure")
    public ResponseEntity<ApiResponse> update(@PathVariable @Positive(message = "id phải lớn hơn 0") Long id,
            @Valid @RequestBody DepartureRequest request) {
        DepartureResponse resp = departureService.update(id, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật thành công")
                .data(resp)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete departure")
    public ResponseEntity<ApiResponse> delete(@PathVariable @Positive(message = "id phải lớn hơn 0") Long id) {
        departureService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa thành công")
                .data(null)
                .build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable @Positive(message = "id phải lớn hơn 0") Long id,
            @RequestParam("value") @Pattern(regexp = "OPEN|CLOSED", message = "value phải là OPEN hoặc CLOSED") String value) {
        DepartureResponse resp = departureService.updateStatus(id, value);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật trạng thái")
                .data(resp)
                .build());
    }
}

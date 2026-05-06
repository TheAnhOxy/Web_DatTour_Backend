package com.tour.core.controller;

import com.tour.core.dto.request.TransportationRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.TransportationResponse;
import com.tour.core.service.TransportationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core/transportations")
@Tag(name = "Transportation", description = "Quản lý phương tiện vận chuyển")
@RequiredArgsConstructor
public class TransportationController {

    private final TransportationService transportationService;

    @GetMapping
    @Operation(summary = "Get all transportations")
    public ResponseEntity<ApiResponse> getAll() {
        List<TransportationResponse> transportations = transportationService.getAll();
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách vận chuyển thành công")
                .data(transportations)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transportation by id")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        TransportationResponse transportation = transportationService.getById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy vận chuyển thành công")
                .data(transportation)
                .build());
    }

    @PostMapping
    @Operation(summary = "Create transportation")
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody TransportationRequest request) {
        TransportationResponse transportation = transportationService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.builder()
                .status(201)
                .message("Thêm vận chuyển thành công")
                .data(transportation)
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update transportation")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @Valid @RequestBody TransportationRequest request) {
        TransportationResponse transportation = transportationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật vận chuyển thành công")
                .data(transportation)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transportation")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        transportationService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa vận chuyển thành công")
                .data(null)
                .build());
    }
}

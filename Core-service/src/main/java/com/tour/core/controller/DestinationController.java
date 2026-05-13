package com.tour.core.controller;

import com.tour.core.dto.request.DestinationRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.DestinationResponse;
import com.tour.core.service.DestinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/core/destinations")
@Tag(name = "Destination", description = "Quản lý điểm đến")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping
    @Operation(summary = "Get all destinations with pagination")
    public ResponseEntity<ApiResponse> getAll(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<DestinationResponse> destinations = destinationService.getAll(page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách điểm đến thành công")
                .data(destinations)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get destination by id")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        DestinationResponse destination = destinationService.getById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy điểm đến thành công")
                .data(destination)
                .build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search destinations by keyword (city or country)")
    public ResponseEntity<ApiResponse> search(
            @RequestParam String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<DestinationResponse> destinations = destinationService.search(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Tìm kiếm điểm đến thành công")
                .data(destinations)
                .build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create destination")
    public ResponseEntity<ApiResponse> create(
            @RequestParam("cityName") String cityName,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam("country") String country,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestPart(value = "file", required = false) MultipartFile file) {
            DestinationRequest request = DestinationRequest.builder()
                .cityName(cityName)
                .region(region)
                .country(country)
                .imageUrl(imageUrl)
                .build();
            DestinationResponse destination = destinationService.create(request, file);
            return ResponseEntity.status(201).body(ApiResponse.builder()
                .status(201)
                .message("Thêm điểm đến thành công")
                .data(destination)
                .build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update destination")
    public ResponseEntity<ApiResponse> update(
            @PathVariable Long id,
            @RequestParam("cityName") String cityName,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam("country") String country,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestPart(value = "file", required = false) MultipartFile file) {
            DestinationRequest request = DestinationRequest.builder()
                .cityName(cityName)
                .region(region)
                .country(country)
                .imageUrl(imageUrl)
                .build();
            DestinationResponse destination = destinationService.update(id, request, file);
            return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật điểm đến thành công")
                .data(destination)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete destination")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        destinationService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa điểm đến thành công")
                .data(null)
                .build());
    }
}

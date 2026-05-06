package com.tour.core.controller;

import com.tour.core.dto.request.CategoryRequest;
import com.tour.core.dto.response.ApiResponse;
import com.tour.core.dto.response.CategoryResponse;
import com.tour.core.service.TourCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Tour Category", description = "Quản lý danh mục tour")
@RequiredArgsConstructor
public class TourCategoryController {

    private final TourCategoryService tourCategoryService;

    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse> getAll() {
        List<CategoryResponse> categories = tourCategoryService.getAll();
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách danh mục thành công")
                .data(categories)
                .build());
    }

    @GetMapping("/top")
    @Operation(summary = "Get top categories by tour count")
    public ResponseEntity<ApiResponse> getTop(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<CategoryResponse> categories = tourCategoryService.getTopCategories(limit);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy top danh mục thành công")
                .data(categories)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by id")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        CategoryResponse category = tourCategoryService.getById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh mục thành công")
                .data(category)
                .build());
    }

    @PostMapping
    @Operation(summary = "Create category")
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = tourCategoryService.create(request);
        return ResponseEntity.status(201).body(ApiResponse.builder()
                .status(201)
                .message("Thêm danh mục thành công")
                .data(category)
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = tourCategoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Cập nhật danh mục thành công")
                .data(category)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        tourCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Xóa danh mục thành công")
                .data(null)
                .build());
    }
}
package com.tour.core.controller;

import com.tour.core.dto.response.ApiResponse;
import com.tour.core.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Thống kê và báo cáo Admin")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Lấy tổng quan dashboard", description = "Trả về thống kê tổng hợp các chỉ số quan trọng")
    public ResponseEntity<ApiResponse> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Tổng quan dashboard")
                .data(statsService.getDashboardSummary())
                .build());
    }

    @GetMapping("/tours")
    @Operation(summary = "Thống kê tour", description = "Trả về các chỉ số thống kê về tour")
    public ResponseEntity<ApiResponse> getTourStats() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Thống kê tour")
                .data(statsService.getTourStats())
                .build());
    }

    @GetMapping("/promotions")
    @Operation(summary = "Thống kê khuyến mãi", description = "Trả về các chỉ số thống kê về khuyến mãi")
    public ResponseEntity<ApiResponse> getPromotionStats() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Thống kê khuyến mãi")
                .data(statsService.getPromotionStats())
                .build());
    }

    @GetMapping("/wishlists")
    @Operation(summary = "Thống kê wishlist", description = "Trả về các chỉ số thống kê về wishlist")
    public ResponseEntity<ApiResponse> getWishlistStats() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Thống kê wishlist")
                .data(statsService.getWishlistStats())
                .build());
    }

    @GetMapping("/departures/upcoming")
    @Operation(summary = "Lịch khởi hành sắp tới", description = "Lấy danh sách các lịch khởi hành sắp tới")
    public ResponseEntity<ApiResponse> getUpcomingDepartures(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lịch khởi hành sắp tới")
                .data(statsService.getUpcomingDepartures(limit))
                .build());
    }

    @GetMapping("/departures/nearly-full")
    @Operation(summary = "Lịch khởi hành sắp kín", description = "Lấy danh sách các lịch khởi hành có tỷ lệ đặt chỗ >= 90%")
    public ResponseEntity<ApiResponse> getNearlyFullDepartures() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lịch khởi hành sắp kín")
                .data(statsService.getNearlyFullDepartures())
                .build());
    }

    @PostMapping("/dashboard/refresh")
    @Operation(summary = "Refresh cache dashboard", description = "Xóa cache và tải dữ liệu mới nhất")
    @CacheEvict(value = "dashboard", allEntries = true)
    public ResponseEntity<ApiResponse> refreshDashboard() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Dashboard đã được làm mới")
                .data(statsService.getDashboardSummary())
                .build());
    }
}

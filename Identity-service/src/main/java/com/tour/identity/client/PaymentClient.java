package com.tour.identity.client;

import com.tour.identity.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    // Gọi API lấy toàn bộ danh sách thanh toán của Admin
    @GetMapping("/payments/admin/all")
    ApiResponse getAllPayments();
}
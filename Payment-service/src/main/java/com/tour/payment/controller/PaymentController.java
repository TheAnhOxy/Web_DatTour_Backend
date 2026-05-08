package com.tour.payment.controller;


import com.tour.payment.dto.response.ApiResponse;
import com.tour.payment.dto.response.PaymentResponse;
import com.tour.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {


    @GetMapping
    public String getHello(){
        return "hello payment";
    }

    private final PaymentService paymentService;


    @GetMapping("/callback")
        public ApiResponse gatewayCallback(@RequestParam("gateway") String gateway,
                           @RequestParam java.util.Map<String, String> params) {

        paymentService.processCallback(gateway, params);
        return ApiResponse.builder()
                .status(200)
                .message("Đã xử lý callback từ cổng thanh toán")
            .data("OK")
                .build();
    }

    // Lấy thông tin thanh toán theo mã đơn hàng
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    // Lấy chi tiết một giao dịch theo transactionId
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransaction(@PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.getPaymentByTransactionId(transactionId));
    }



}
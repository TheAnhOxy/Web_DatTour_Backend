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


    @GetMapping("/vnpay-callback")
    public ApiResponse vnpayCallback(@RequestParam("vnp_TxnRef") String txnRef,
                                     @RequestParam("vnp_ResponseCode") String responseCode) {

        String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
        paymentService.processCallback(txnRef, status);
        return ApiResponse.builder()
                .status(200)
                .message("Đã xử lý callback từ cổng thanh toán")
                .data(status)
                .build();
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách thanh toán thành công")
                .data(paymentService.getAllPaymentDetails())
                .build());
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

//    @PostMapping
//    public ResponseEntity<ApiResponse> create(@RequestBody FoodRequest request) {
//        return ResponseEntity.status(201).body(ApiResponse.builder()
//                .status(201)
//                .message("Thêm món thành công")
//                .data(foodService.createFood(request))
//                .build());
//    }

}
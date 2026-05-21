package com.tour.payment.controller;


import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.tour.payment.dto.request.SePayWebhookRequest;
import com.tour.payment.dto.response.ApiResponse;
import com.tour.payment.dto.response.PaymentResponse;
import com.tour.payment.service.PaymentService;
import com.tour.payment.service.stripe.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final StripeService stripeService;

    @GetMapping
    public String getHello() {
        return "hello payment";
    }

    @GetMapping("/callback")
    public ApiResponse gatewayCallback(@RequestParam("gateway") String gateway,
                                       @RequestParam Map<String, String> params) {
        paymentService.processCallback(gateway, params);
        return ApiResponse.builder()
                .status(200)
                .message("Đã xử lý callback từ cổng thanh toán")
                .data("OK")
                .build();
    }

    @PostMapping("/sepay-webhook")
    public ResponseEntity<ApiResponse> handleSePayWebhook(@RequestBody SePayWebhookRequest webhookData) {
        try {
            String transactionId = webhookData.resolveTransactionId();
            String status        = webhookData.resolveStatus();

            if (transactionId == null || transactionId.isBlank()) {
                log.warn("[SePay Webhook] Bỏ qua: không xác định được transactionId từ payload");
                return ResponseEntity.ok(ApiResponse.builder().status(200).message("Ignored: no transactionId").build());
            }

            Map<String, String> params = new HashMap<>();
            params.put("transactionId", transactionId);
            params.put("status", status);
            if (webhookData.getIdempotencyKey() != null && !webhookData.getIdempotencyKey().isBlank()) {
                params.put("idempotencyKey", webhookData.getIdempotencyKey());
            }

            log.info("[SePay Webhook] transactionId={} status={}", transactionId, status);
            paymentService.processCallback("SEPAY", params);
            return ResponseEntity.ok(ApiResponse.builder()
                    .status(200).message("Xử lý SePay thành công").build());
        } catch (Exception ex) {
            log.warn("[SePay Webhook] Lỗi: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .status(400).message("Lỗi SePay: " + ex.getMessage()).build());
        }
    }

    /**
     * Endpoint nhận webhook thật từ Stripe.
     * - Nhận raw body (String) để verify chữ ký Stripe-Signature.
     * - Xử lý 2 loại event: checkout.session.completed → SUCCESS
     *                        checkout.session.expired  → FAILED
     */
    @PostMapping(value = "/stripe-webhook", consumes = "application/json")
    public ResponseEntity<ApiResponse> handleStripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            com.stripe.model.Event event = stripeService.constructWebhookEvent(rawPayload, sigHeader);

            String eventType = event.getType();
            log.info("[Stripe Webhook] Nhận event: {}", eventType);

            if ("checkout.session.completed".equals(eventType)
                    || "checkout.session.expired".equals(eventType)) {

                EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
                Session session = (Session) deserializer.getObject()
                        .orElseThrow(() -> new RuntimeException("Không thể parse Stripe Session từ event"));

                String transactionId = session.getId(); // cs_test_...
                String status = "checkout.session.completed".equals(eventType) ? "SUCCESS" : "FAILED";
                String idempotencyKey = "STRIPE:" + transactionId + ":" + eventType;

                Map<String, String> params = new HashMap<>();
                params.put("transactionId", transactionId);
                params.put("status", status);
                params.put("idempotencyKey", idempotencyKey);

                paymentService.processCallback("STRIPE", params);
            } else {
                log.info("[Stripe Webhook] Bỏ qua event không xử lý: {}", eventType);
            }

            return ResponseEntity.ok(ApiResponse.builder()
                    .status(200).message("Webhook Stripe xử lý thành công").build());

        } catch (RuntimeException ex) {
            log.warn("[Stripe Webhook] Lỗi xử lý: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.builder()
                    .status(400).message(ex.getMessage()).build());
        }
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



}
package com.tour.payment.service.gateway;

import com.tour.payment.entity.Payment;
import com.tour.payment.entity.PaymentMethod;
import com.tour.payment.service.callback.PaymentCallbackData;
import com.tour.payment.service.sepay.SePayService;
import org.springframework.stereotype.Component;

import com.tour.payment.service.state.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;

@Component
@lombok.RequiredArgsConstructor
public class SePayPaymentGatewayStrategy implements PaymentGatewayStrategy {

    private final SePayService sePayService;

    @Override
    public String getGateway() {
        return "SEPAY";
    }

    @Override
    public Payment createPayment(Map<String, Object> message, PaymentMethod method) {
        String bookingCode = message.get("bookingCode").toString();
        double amount = Double.parseDouble(message.get("totalAmount").toString());
        // Nội dung CK = "SEVQR" + bookingCode để SePay nhận diện tự động
        String transferContent = "SEVQR" + bookingCode;
        String paymentUrl = sePayService.generateQrUrl(amount, transferContent);

        return Payment.builder()
                .bookingId(Long.valueOf(message.get("bookingId").toString()))
                .transactionId(transferContent) // khớp với nội dung CK người dùng nhập
                .amount(new BigDecimal(message.get("totalAmount").toString()))
                .status(PaymentStatus.PENDING.name())
                .gateway(getGateway())
                .paymentMethod(method)
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    public PaymentCallbackData parseCallback(Map<String, String> params) {
        String transactionId = params.get("transactionId");
        String status = params.get("status");
        String idempotencyKey = params.get("idempotencyKey");
        String amountRaw = params.get("amount");
        if (transactionId == null || status == null) {
            throw new RuntimeException("Missing callback params for SePay");
        }

        java.math.BigDecimal amount = null;
        if (amountRaw != null && !amountRaw.isBlank()) {
            try {
                amount = new java.math.BigDecimal(amountRaw);
            } catch (NumberFormatException ex) {
                amount = null;
            }
        }

        return PaymentCallbackData.builder()
                .transactionId(transactionId)
                .status(status)
                .idempotencyKey(idempotencyKey == null ? buildIdempotencyKey(transactionId, status) : idempotencyKey)
            .amount(amount)
                .build();
    }

    private String buildIdempotencyKey(String transactionId, String status) {
        return getGateway() + ":" + transactionId + ":" + status;
    }
}

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
        String paymentUrl = sePayService.generateQrUrl(amount, bookingCode);

        return Payment.builder()
                .bookingId(Long.valueOf(message.get("bookingId").toString()))
                .transactionId(bookingCode)
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
        if (transactionId == null || status == null) {
            throw new RuntimeException("Missing callback params for SePay");
        }

        return PaymentCallbackData.builder()
                .transactionId(transactionId)
                .status(status)
                .idempotencyKey(idempotencyKey == null ? buildIdempotencyKey(transactionId, status) : idempotencyKey)
                .build();
    }

    private String buildIdempotencyKey(String transactionId, String status) {
        return getGateway() + ":" + transactionId + ":" + status;
    }
}

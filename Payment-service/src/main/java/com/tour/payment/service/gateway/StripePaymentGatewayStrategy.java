package com.tour.payment.service.gateway;

import com.stripe.model.checkout.Session;
import com.tour.payment.entity.Payment;
import com.tour.payment.entity.PaymentMethod;
import com.tour.payment.service.callback.PaymentCallbackData;
import com.tour.payment.service.state.PaymentStatus;
import com.tour.payment.service.stripe.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StripePaymentGatewayStrategy implements PaymentGatewayStrategy {

    private final StripeService stripeService;

    @Override
    public String getGateway() {
        return "STRIPE";
    }

    /**
     * Tạo Stripe Checkout Session thật.
     * transactionId = Stripe Session ID (cs_test_...)
     * paymentUrl    = Stripe hosted checkout URL
     */
    @Override
    public Payment createPayment(Map<String, Object> message, PaymentMethod method) {
        Long bookingId = Long.valueOf(message.get("bookingId").toString());
        String bookingCode = message.get("bookingCode").toString();
        BigDecimal amount = new BigDecimal(message.get("totalAmount").toString());

        Session session = stripeService.createCheckoutSession(bookingId, bookingCode, amount);

        return Payment.builder()
                .bookingId(bookingId)
                .transactionId(session.getId())
                .amount(amount)
                .status(PaymentStatus.PENDING.name())
                .gateway(getGateway())
                .paymentMethod(method)
                .paymentUrl(session.getUrl())
                .build();
    }

    /**
     * Parse callback params sau khi controller đã verify webhook Stripe.
     * Controller truyền vào: transactionId (session ID), status, idempotencyKey.
     */
    @Override
    public PaymentCallbackData parseCallback(Map<String, String> params) {
        String transactionId = params.get("transactionId");
        String status = params.get("status");
        String idempotencyKey = params.get("idempotencyKey");

        if (transactionId == null || status == null) {
            throw new RuntimeException("Thiếu thông tin callback từ Stripe");
        }

        return PaymentCallbackData.builder()
                .transactionId(transactionId)
                .status(status)
                .idempotencyKey(idempotencyKey != null ? idempotencyKey
                        : getGateway() + ":" + transactionId + ":" + status)
                .build();
    }
}

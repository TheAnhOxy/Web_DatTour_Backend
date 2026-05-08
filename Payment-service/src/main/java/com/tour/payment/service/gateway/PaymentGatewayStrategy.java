package com.tour.payment.service.gateway;

import com.tour.payment.entity.Payment;
import com.tour.payment.entity.PaymentMethod;

import java.time.LocalDateTime;
import java.util.Map;

public interface PaymentGatewayStrategy {

    String getGateway();

    Payment createPayment(Map<String, Object> message, PaymentMethod method);

    default void applyCallback(Payment payment, String status) {
        payment.setStatus(status);
        payment.setPaidAt(LocalDateTime.now());
    }
}

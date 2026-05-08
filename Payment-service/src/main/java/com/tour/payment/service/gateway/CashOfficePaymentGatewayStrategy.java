package com.tour.payment.service.gateway;

import com.tour.payment.entity.Payment;
import com.tour.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CashOfficePaymentGatewayStrategy implements PaymentGatewayStrategy {

    @Override
    public String getGateway() {
        return "CASH_OFFICE";
    }

    @Override
    public Payment createPayment(Map<String, Object> message, PaymentMethod method) {
        String bookingCode = message.get("bookingCode").toString();

        return Payment.builder()
                .bookingId(Long.valueOf(message.get("bookingId").toString()))
                .transactionId(bookingCode)
                .amount(new BigDecimal(message.get("totalAmount").toString()))
                .status("PENDING")
                .gateway(getGateway())
                .paymentMethod(method)
                .paymentUrl(null)
                .build();
    }
}

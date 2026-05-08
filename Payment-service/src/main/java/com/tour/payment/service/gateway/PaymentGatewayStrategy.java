package com.tour.payment.service.gateway;

import com.tour.payment.entity.Payment;
import com.tour.payment.entity.PaymentMethod;
import com.tour.payment.service.callback.PaymentCallbackData;

import java.util.Map;

public interface PaymentGatewayStrategy {

    String getGateway();

    Payment createPayment(Map<String, Object> message, PaymentMethod method);

    PaymentCallbackData parseCallback(Map<String, String> params);
}

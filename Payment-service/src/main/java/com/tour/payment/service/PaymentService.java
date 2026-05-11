package com.tour.payment.service;

import com.tour.payment.dto.response.PaymentDetailResponse;
import com.tour.payment.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse getPaymentByBookingId(Long bookingId);

    PaymentResponse getPaymentByTransactionId(String transactionId);
    void processCallback(String gateway, java.util.Map<String, String> params);
    List<PaymentDetailResponse> getAllPaymentDetails();

}

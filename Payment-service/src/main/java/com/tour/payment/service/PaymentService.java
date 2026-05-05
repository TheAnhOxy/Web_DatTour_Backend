package com.tour.payment.service;

import com.tour.payment.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse getPaymentByBookingId(Long bookingId);

    PaymentResponse getPaymentByTransactionId(String transactionId);

}

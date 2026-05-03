package com.tour.service;

import com.tour.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse getPaymentByBookingId(Long bookingId);

    PaymentResponse getPaymentByTransactionId(String transactionId);

}

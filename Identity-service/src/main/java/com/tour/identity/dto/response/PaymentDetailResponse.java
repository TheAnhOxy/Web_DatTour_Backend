package com.tour.identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentDetailResponse {
    // 1. Dữ liệu từ Payment Service
    private String transactionId;
    private BigDecimal amount;
    private String paymentStatus; // SUCCESS, FAILED, PENDING
    private LocalDateTime paidAt;
    private String paymentUrl;
    private String gateway;
    private String paymentMethodName;

    // 2. Dữ liệu từ Booking Service (Gộp qua bookingId)
    private Object bookingInfo;

    // 3. Dữ liệu từ Identity Service (Gộp qua userId)
    private UserResponse customerInfo;
}
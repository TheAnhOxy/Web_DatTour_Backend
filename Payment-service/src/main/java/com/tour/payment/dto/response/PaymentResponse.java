package com.tour.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private String transactionId;
    private String gateway;
    private String paymentUrl;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
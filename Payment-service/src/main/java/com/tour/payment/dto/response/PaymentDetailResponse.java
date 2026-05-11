package com.tour.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDetailResponse {
    private String transactionId;
    private BigDecimal amount;
    private String paymentStatus;
    private LocalDateTime paidAt;
    private String paymentUrl;
    private String paymentMethodName;

    // THÊM DÒNG NÀY: Mã để liên kết với Booking Service
    private Long bookingId;

    // Các trường này để null ở Payment Service,
    // Identity Service sẽ lo việc đổ dữ liệu vào sau.
    private Object bookingDetails;
    private Object customerDetails;
}
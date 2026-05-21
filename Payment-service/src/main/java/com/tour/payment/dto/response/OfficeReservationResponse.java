package com.tour.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeReservationResponse {
    private String bookingCode;
    private Long bookingId;
    private String gateway;
    private String status;
    private String paymentDueAt;
    private boolean emailSent;
}

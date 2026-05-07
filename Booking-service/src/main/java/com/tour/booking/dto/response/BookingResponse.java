package com.tour.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private String bookingCode;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private String message;
    private String tourTitle;
    private String startDate;
    private Map<String, Object> priceDetail;
}

package com.tour.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerResponseDTO {
    private Long id;
    private String fullName;
    private String idCardNumber;
    private String gender;
    private String ageGroup;

    private String bookingCode;
    private String status;
    private String tourTitle;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
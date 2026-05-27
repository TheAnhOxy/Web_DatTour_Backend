package com.tour.booking.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestBookingResponseDTO {
    private Long id;
    private String bookingCode;
    private Long departureId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String status;
    private String paymentMethod;
    private LocalDateTime paymentDueAt;
    private LocalDateTime createdAt;


    private String contactName;
    private String contactEmail;
    private String contactPhone;

    private Map<String, Object> priceSnapshot;
    private Map<String, Object> promotionSnapshot;

    private List<GuestPassengerDTO> passengers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuestPassengerDTO {
        private Long id;
        private String fullName;
        private String gender;
        private String ageGroup;
        private String idCardNumber;
        private String passportNumber;
        private String dob;
    }
}
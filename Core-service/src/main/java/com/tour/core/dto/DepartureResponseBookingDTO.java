package com.tour.core.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureResponseBookingDTO {
    private Long id;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxSlots;
    private Integer bookedSlots;

    // Thông tin Tour
    private Long tourId;
    private String tourTitle;

    // Sử dụng tên DTO mới của tiền bối
    private DestinationBookingDTO destination;

    // Thông tin Pickup
    private String pickupName;
    private String pickupAddress;
    private LocalDateTime pickupTime;

    // Sử dụng tên DTO mới của tiền bối
    private PriceConfigBookingDto priceConfig;
}
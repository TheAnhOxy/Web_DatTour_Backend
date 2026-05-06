package com.tour.core.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxSlots;
    private Integer bookedSlots;
    private String status;

    // Pickup fields (match Departure entity)
    private String pickupName;
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private LocalDateTime pickupTime;
}

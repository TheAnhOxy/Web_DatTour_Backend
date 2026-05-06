package com.tour.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer maxSlots;

    private Integer bookedSlots;

    private String pickupName;

    private String pickupAddress;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private LocalDateTime pickupTime;

    private String status;
}

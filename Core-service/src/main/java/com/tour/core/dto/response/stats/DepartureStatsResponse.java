package com.tour.core.dto.response.stats;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DepartureStatsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long departureId;
    private String tourTitle;
    private String tourSlug;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer maxSlots;
    private Integer bookedSlots;
    private Integer availableSlots;
    private Double occupancyRate;
    private String status;
}

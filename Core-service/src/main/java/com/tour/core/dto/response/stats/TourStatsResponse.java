package com.tour.core.dto.response.stats;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder
public class TourStatsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long totalTours;
    private Long activeTours;
    private Long inactiveTours;
    private Long hotTours;
    private Map<String, Long> toursByCategory;

    private Long totalDepartures;
    private Long openDepartures;

    private Long totalSlots;
    private Long bookedSlots;
    private Long availableSlots;
    private Double occupancyRate;
}

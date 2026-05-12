package com.tour.core.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TourSearchEvent {
    private Long tourId;
    private String title;
    private String slug;
    private BigDecimal basePrice;
    private List<String> destinations; // Tên các thành phố
    private List<DepartureEvent> departures;
}
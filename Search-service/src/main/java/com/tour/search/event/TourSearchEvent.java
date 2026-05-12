package com.tour.search.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourSearchEvent {
    private Long tourId;
    private String title;
    private String slug;
    private BigDecimal basePrice;
    private List<String> destinations; // Tên các thành phố
    private List<DepartureEvent> departures;
}
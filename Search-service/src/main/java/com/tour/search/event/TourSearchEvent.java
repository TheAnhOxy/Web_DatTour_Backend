package com.tour.search.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private List<String> destinations;

    // 💡 THÊM CÁC TRƯỜNG NÀY VÀO EVENT
    private Boolean isHot;
    private String coverImageUrl;
    private String categoryName;
    private Integer durationDays;
    private String region;
    private Long departureId;
    private LocalDateTime departureStartDate;
    private String pickupName;
    private String pickupAddress;
    private LocalDateTime pickupTime;
    private BigDecimal rating;
    private Integer reviewCount;
    private String transportationType;
    private List<DepartureEvent> departures;
}
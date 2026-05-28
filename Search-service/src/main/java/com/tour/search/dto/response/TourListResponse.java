package com.tour.search.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String slug;

    private BigDecimal basePrice;

    private String status;

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
}

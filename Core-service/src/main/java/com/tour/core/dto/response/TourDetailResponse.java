package com.tour.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String slug;

    private String description;

    private Integer durationDays;

    private String status;

    private Boolean isHot;

    private BigDecimal basePrice;

    private Long categoryId;

    private String categoryName;

    private Long transportationId;

    private String transportationType;

    private LocalDateTime createdAt;

    private List<TourImageResponse> images;

    private List<DestinationResponse> destinations;

    private List<DepartureResponse> departures;
}

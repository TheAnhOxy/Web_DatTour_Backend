package com.tour.core.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourRequest {

    @NotBlank(message = "Tiêu đề không được trống")
    private String title;

    @NotBlank(message = "Mô tả không được trống")
    private String description;

    @NotNull(message = "Số ngày không được trống")
    private Integer durationDays;

    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private Boolean isHot = false;

    @NotNull(message = "Giá cơ bản không được trống")
    private BigDecimal basePrice;

    @NotNull(message = "Danh mục không được trống")
    private Long categoryId;

    @NotNull(message = "Phương tiện không được trống")
    private Long transportationId;

    private List<Long> destinationIds;
    private List<DepartureRequest> departures;

    private String overview;
    private String itinerary;
    private String inclusions;
    private String exclusions;
    private String policies;
    private BigDecimal rating;
    private Integer reviewCount;

    @Valid
    private List<TourImageRequest> images;
}

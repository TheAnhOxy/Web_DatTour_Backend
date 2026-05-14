package com.tour.core.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceConfigBookingDto {
    private Long id;
    private Long departureId;
    private BigDecimal adultPrice;
    private BigDecimal child1014Price;
    private BigDecimal child49Price;
    private BigDecimal babyPrice;
}
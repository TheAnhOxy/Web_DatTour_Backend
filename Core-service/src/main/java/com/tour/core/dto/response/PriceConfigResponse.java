package com.tour.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceConfigResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long departureId;
    private BigDecimal adultPrice;
    private BigDecimal child1014Price;
    private BigDecimal child49Price;
    private BigDecimal babyPrice;
}

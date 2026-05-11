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
public class PriceBreakdownItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String passengerType; // ADULT / CHILD_10_14 / CHILD_4_9 / BABY
    private int count;
    private BigDecimal baseUnitPrice;
    private BigDecimal finalUnitPrice;
    private BigDecimal discountAmount;
    private BigDecimal total;
}

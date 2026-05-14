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
public class PriceCalculateResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long departureId;
    private String tourTitle;
    private LocalDateTime departureDate;
    private List<String> appliedRules;
    private List<PriceBreakdownItem> breakdown;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal totalAmount;
    private int totalPassengers;
    private int availableSlots;
}

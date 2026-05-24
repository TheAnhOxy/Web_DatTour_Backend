package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryDTO {
    
    @JsonProperty("totalBookings")
    private Integer totalBookings;
    
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;
    
    @JsonProperty("totalPaidAmount")
    private BigDecimal totalPaidAmount;
    
    @JsonProperty("byStatus")
    private Map<String, Integer> byStatus;
}

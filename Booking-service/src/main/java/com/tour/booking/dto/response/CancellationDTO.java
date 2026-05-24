package com.tour.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationDTO {
    
    @JsonProperty("cancellationId")
    private Long cancellationId;
    
    @JsonProperty("bookingId")
    private Long bookingId;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("refundAmount")
    private BigDecimal refundAmount;
    
    @JsonProperty("cancelledAt")
    private LocalDateTime cancelledAt;
}

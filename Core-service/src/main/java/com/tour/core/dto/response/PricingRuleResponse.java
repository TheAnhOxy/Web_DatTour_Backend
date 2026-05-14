package com.tour.core.dto.response;

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
public class PricingRuleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long departureId;
    private String ruleName;
    private String ruleType;
    private String adjustmentType;
    private BigDecimal adjustmentValue;
    private Integer minDaysBefore;
    private Integer maxDaysBefore;
    private Integer minSlotsLeft;
    private Integer maxSlotsLeft;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer priority;
    private Boolean isActive;
}

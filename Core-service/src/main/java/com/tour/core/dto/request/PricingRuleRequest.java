package com.tour.core.dto.request;

import jakarta.validation.constraints.*;
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
public class PricingRuleRequest {

    @NotBlank(message = "Tên rule không được trống")
    private String ruleName;

    @NotBlank(message = "Loại rule không được trống")
    private String ruleType; // EARLY_BIRD / LAST_MINUTE / SLOT_BASED

    @NotBlank(message = "Loại điều chỉnh không được trống")
    private String adjustmentType; // PERCENT / FIXED

    @NotNull(message = "Giá trị điều chỉnh không được trống")
    @DecimalMin(value = "0", message = "Giá trị phải >= 0")
    private BigDecimal adjustmentValue;

    private Integer minDaysBefore;
    private Integer maxDaysBefore;
    private Integer minSlotsLeft;
    private Integer maxSlotsLeft;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    @NotNull
    @Min(1)
    private Integer priority;

    @Builder.Default
    private Boolean isActive = true;
}

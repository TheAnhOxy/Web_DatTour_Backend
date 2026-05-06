package com.tour.core.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class PromotionRequest {

    @NotBlank(message = "Mã promotion không được để trống")
    private String code;

    @NotNull
    @DecimalMin(value = "0", message = "discountPercent phải lớn hơn hoặc bằng 0")
    @DecimalMax(value = "100", message = "discountPercent phải nhỏ hơn hoặc bằng 100")
    private BigDecimal discountPercent;

    private BigDecimal maxDiscount;

    @NotNull
    @Min(value = 1, message = "usageLimit phải lớn hơn hoặc bằng 1")
    private Integer usageLimit;

    @NotNull(message = "validFrom không được để trống")
    private LocalDateTime validFrom;

    @NotNull(message = "validTo không được để trống")
    private LocalDateTime validTo;
}

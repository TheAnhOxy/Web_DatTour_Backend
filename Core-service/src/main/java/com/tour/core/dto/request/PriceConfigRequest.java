package com.tour.core.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceConfigRequest {
    @NotNull(message = "Giá người lớn không được để trống")
    @DecimalMin(value = "0.0", message = "Giá người lớn phải >= 0")
    private BigDecimal adultPrice;

    @NotNull(message = "Giá trẻ em tuổi từ 10-14 không được để trống")
    @DecimalMin(value = "0.0", message = "Giá trẻ em tuổi từ 10-14 phải >= 0")
    private BigDecimal child1014Price;

    @NotNull(message = "Giá trẻ em tuổi từ 4-9 không được để trống")
    @DecimalMin(value = "0.0", message = "Giá trẻ em tuổi từ 4-9 phải >= 0")
    private BigDecimal child49Price;

    @NotNull(message = "Giá em bé không được để trống")
    @DecimalMin(value = "0.0", message = "Giá em bé phải >= 0")
    private BigDecimal babyPrice;
}

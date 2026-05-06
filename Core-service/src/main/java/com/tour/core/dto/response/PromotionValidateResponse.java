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
public class PromotionValidateResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private Boolean isValid;
    private String message;
    private BigDecimal discountPercent;
    private BigDecimal maxDiscount;
}

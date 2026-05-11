package com.tour.core.dto.response.stats;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class PromotionUsageItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private Integer usedCount;
    private Integer usageLimit;
    private Double usageRate;
    private Boolean isActive;
}

package com.tour.core.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculateRequest {

    @NotNull
    private Long departureId;

    @Min(0)
    @Builder.Default
    private int adultCount = 1;

    @Min(0)
    @Builder.Default
    private int child1014Count = 0;

    @Min(0)
    @Builder.Default
    private int child49Count = 0;

    @Min(0)
    @Builder.Default
    private int babyCount = 0;

    @AssertTrue(message = "Phải có ít nhất 1 hành khách")
    public boolean isValidPassengerCount() {
        return adultCount + child1014Count + child49Count + babyCount >= 1;
    }
}

package com.tour.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationRequest {

    @NotBlank(message = "Tên thành phố không được trống")
    private String cityName;

    private String region;

    @NotBlank(message = "Tên quốc gia không được trống")
    private String country;

    private String imageUrl;
}

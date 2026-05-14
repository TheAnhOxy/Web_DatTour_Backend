package com.tour.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationBookingDTO {

    private Long id;
    private String cityName;
    private String region;
    private String country;
    private String imageUrl;
}

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
public class TourImageRequest {

    @NotBlank(message = "URL ảnh không được trống")
    private String imageUrl;

    private Boolean isCover;

    private Integer sortOrder;
}

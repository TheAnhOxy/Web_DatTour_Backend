package com.tour.core.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourImageReorderRequest {

    @NotNull(message = "Danh sách imageIds không được null")
    @NotEmpty(message = "Danh sách imageIds không được rỗng")
    private List<Long> imageIds;
}

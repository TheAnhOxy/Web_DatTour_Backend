package com.tour.core.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartureRequest {
    @NotNull(message = "tourId không được để trống")
    @Positive(message = "tourId phải lớn hơn 0")
    private Long tourId;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    @NotNull(message = "Số chỗ tối đa không được để trống")
    @Min(value = 1, message = "Số chỗ tối đa phải lớn hơn 0")
    private Integer maxSlots;

    private Integer bookedSlots;

    @Pattern(regexp = "OPEN|CLOSED", message = "Trạng thái phải là OPEN hoặc CLOSED")
    private String status;

    // Pickup fields (match Departure entity)
    private String pickupName;

    private String pickupAddress;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private LocalDateTime pickupTime;

    @Valid
    private PriceConfigRequest priceConfig;
}

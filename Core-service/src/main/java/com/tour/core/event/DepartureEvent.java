package com.tour.core.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DepartureEvent {
    private Long id;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}

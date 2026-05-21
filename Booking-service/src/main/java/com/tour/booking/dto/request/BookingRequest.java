package com.tour.booking.dto.request;

import com.tour.booking.dto.PassengerDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingRequest {
    private Long userId;
    private Long departureId;
    private List<PassengerDTO> passengers;
    private String note;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
}
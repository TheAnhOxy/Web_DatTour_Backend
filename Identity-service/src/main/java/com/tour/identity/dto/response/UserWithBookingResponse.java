package com.tour.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserWithBookingResponse {
    private UserResponse user;
    private List<Object> bookings; // Object ở đây là các BookingResponse từ Booking Service
}
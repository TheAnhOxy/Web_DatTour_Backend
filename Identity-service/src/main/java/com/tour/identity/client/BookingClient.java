package com.tour.identity.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import com.tour.identity.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "booking-service")
public interface BookingClient {
    @GetMapping("/bookings/by-users")
    ApiResponse getBookingsByUserIds(@RequestParam("userIds") List<Long> userIds);
    @GetMapping("/bookings/by-ids")
    ApiResponse getBookingsByIds(@RequestParam("ids") List<Long> ids);
}
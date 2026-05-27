package com.tour.booking.client;

import com.tour.booking.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "core-service")
public interface CoreClient {
    @GetMapping("/core/departures/details/{id}")
    ApiResponse getDepartureDetails(@PathVariable("id") Long id);
}
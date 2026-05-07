package com.tour.booking.controller;

import com.tour.booking.dto.response.ApiResponse;
import com.tour.booking.dto.response.PassengerResponseDTO;
import com.tour.booking.exception.BusinessException;
import com.tour.booking.repository.PassengerRepository;
import com.tour.booking.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/bookings/passenger")
@RequiredArgsConstructor
public class PassengerController {
    private final PassengerService passengerService;

    @GetMapping("/all")
    public ApiResponse getAll() {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách hành khách thành công")
                .data(passengerService.getAllPassengers())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy chi tiết hành khách thành công")
                .data(passengerService.getPassengerById(id))
                .build();
    }

    @GetMapping("/history/{idCardNumber}")
    public ApiResponse getHistory(@PathVariable String idCardNumber) {

        return ApiResponse.builder()
                .status(200)
                .message("Lấy lịch sử hành khách thành công")
                .data(passengerService.getPassengerHistory(idCardNumber))
                .build();
    }
}
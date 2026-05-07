package com.tour.booking.controller;


import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.request.CancelBookingRequest;
import com.tour.booking.dto.response.ApiResponse;
import com.tour.booking.dto.response.BookingResponse;
import com.tour.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/create")
    public ApiResponse createBooking(@RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);

        return ApiResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Giữ chỗ thành công! Vui lòng thanh toán trong vòng 10 phút.")
                .data(response)
                .build();
    }

    @PostMapping("/cancel")
    public ApiResponse cancel(@RequestBody CancelBookingRequest request) {
        bookingService.cancelBooking(request);
        return ApiResponse.builder()
                .status(200)
                .message("Hủy đơn hàng thành công")
                .build();
    }

    @GetMapping
    public String getHello(){
        return "hello bookings";
    }

//    @PostMapping
//    public ResponseEntity<ApiResponse> create(@RequestBody FoodRequest request) {
//        return ResponseEntity.status(201).body(ApiResponse.builder()
//                .status(201)
//                .message("Thêm món thành công")
//                .data(foodService.createFood(request))
//                .build());
//    }

}
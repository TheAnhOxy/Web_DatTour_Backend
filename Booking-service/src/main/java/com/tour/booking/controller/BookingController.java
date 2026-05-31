package com.tour.booking.controller;


import com.tour.booking.dto.PassengerDTO;
import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.request.CancelBookingRequest;
import com.tour.booking.dto.request.BatchBookingRequest;
import com.tour.booking.dto.response.*;
import com.tour.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ===== POST ENDPOINTS =====

    @PostMapping("/create")
    public ApiResponse createBooking(@Valid @RequestBody BookingRequest request) {
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

    @PostMapping("/batch")
    public ApiResponse getBookingsByIds(
            @RequestBody BatchBookingRequest request) {
        
        Map<Long, BookingDetailResponse> result = 
                bookingService.getBookingsByIds(request.getBookingIds());
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách đơn hàng thành công")
                .data(result)
                .build();
    }

    // ===== GET ENDPOINTS =====

    /**
     * GET /api/v1/bookings/{bookingCode}
     * Get booking details by booking code
     */
    @GetMapping("/admin/{bookingCode}")
    public ApiResponse getByCode(
            @PathVariable String bookingCode) {
        
        BookingDetailResponse response = bookingService.getBookingByCode(bookingCode);
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy chi tiết đơn hàng thành công")
                .data(response)
                .build();
    }

        /**
         * GET /api/v1/bookings/{bookingCode}
         * Get booking details by booking code (public).
         */
        @GetMapping("/{bookingCode}")
        public ApiResponse getByCodePublic(@PathVariable String bookingCode) {
                BookingDetailResponse response = bookingService.getBookingByCode(bookingCode);

                return ApiResponse.builder()
                                .status(200)
                                .message("Lấy chi tiết đơn hàng thành công")
                                .data(response)
                                .build();
        }

    /**
     * GET /api/v1/bookings/id/{bookingId}
     * Get booking details by booking ID
     */
    @GetMapping("/id/{bookingId}")
    public ApiResponse getBookingById(
            @PathVariable Long bookingId) {
        
        BookingDetailResponse response = bookingService.getBookingById(bookingId);
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy chi tiết đơn hàng thành công")
                .data(response)
                .build();
    }

    /**
     * GET /api/v1/bookings/user/{userId}
     * Get all bookings for a user with optional filtering
     * Query params: status, page, limit
     */

    @GetMapping("/admin/users/{userId}")
    public ApiResponse getByUserId(@PathVariable Long userId) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách đơn hàng thành công")
                .data(bookingService.getBookingsByUserId(userId))
                .build();
    }

    @GetMapping("/my-booking/{bookingCode}")
    public ApiResponse getMyBookingByCode(@PathVariable String bookingCode) {
        // Gọi Service xử lý lấy chi tiết (Tầng service nên check thêm xem đơn này có đúng của user đang đăng nhập không nhé)
        BookingDetailResponse response = bookingService.getBookingByCode(bookingCode);

        return ApiResponse.builder()
                .status(200)
                .message("Tìm kiếm đơn hàng thành công")
                .data(response)
                .build();
    }

        @GetMapping("/my-history")
        public ApiResponse getMyBookingHistory(
                @RequestHeader("X-User-Id") Long userId,
                @RequestParam(required = false) String status,
                @RequestParam(defaultValue = "0") Integer page,
                @RequestParam(defaultValue = "10") Integer limit) {

            PaginatedResponse<BookingResponse> response =
                    bookingService.getBookingsByUserId(userId, status, page, limit);

            return ApiResponse.builder()
                    .status(200)
                    .message("Lấy lịch sử đặt tour thành công")
                    .data(response)
                    .build();
        }

    @GetMapping("/user/{userId}")
    public ApiResponse getByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        PaginatedResponse<BookingResponse> response = 
                bookingService.getBookingsByUserId(userId, status, page, limit);
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách đơn hàng thành công")
                .data(response)
                .build();
    }

    /**
     * GET /api/v1/bookings
     * Get all bookings (admin endpoint) with optional filtering
     * Query params: status, paymentMethod, page, limit
     */
    @GetMapping
    public ApiResponse getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        PaginatedResponse<BookingResponse> response = 
                bookingService.getAllBookings(status, paymentMethod, page, limit);
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách đơn hàng thành công")
                .data(response)
                .build();
    }

    @GetMapping("/admin/all")
    public ApiResponse getAll() {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy toàn bộ danh sách đơn hàng thành công")
                .data(bookingService.getAllBookings())
                .build();
    }

    /**
     * GET /api/v1/bookings/{bookingCode}/passengers
     * Get all passengers for a booking
     */
    @GetMapping("/{bookingCode}/passengers")
    public ApiResponse getPassengers(
            @PathVariable String bookingCode) {
        
        List<PassengerDTO> passengers = 
                bookingService.getPassengersByBookingCode(bookingCode);
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách hành khách thành công")
                .data(passengers)
                .build();
    }

    /**
     * GET /api/v1/bookings/{bookingCode}/notes
     * Get all notes for a booking
     */
    @GetMapping("/{bookingCode}/notes")
    public ApiResponse getNotes(
            @PathVariable String bookingCode) {
        
        List<BookingNoteDTO> notes = 
                bookingService.getBookingNotesByBookingCode(bookingCode);
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy ghi chú đơn hàng thành công")
                .data(notes)
                .build();
    }

    /**
     * GET /api/v1/bookings/{bookingCode}/cancellation
     * Get cancellation info for a booking (if exists)
     */
    @GetMapping("/{bookingCode}/cancellation")
    public ApiResponse getCancellation(
            @PathVariable String bookingCode) {
        
        CancellationDTO cancellation = 
                bookingService.getCancellationByBookingCode(bookingCode);
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy thông tin hủy đơn thành công")
                .data(cancellation)
                .build();
    }

    /**
     * GET /api/v1/bookings/user/{userId}/summary
     * Get booking statistics summary for a user
     */
    @GetMapping("/user/{userId}/summary")
    public ApiResponse getUserSummary(
            @PathVariable Long userId) {
        
        BookingSummaryDTO summary = 
                bookingService.getUserBookingSummary(userId);
        
        return ApiResponse.builder()
                .status(200)
                .message("Lấy tóm tắt đơn hàng thành công")
                .data(summary)
                .build();
    }

    // ===== LEGACY ENDPOINTS (DEPRECATED - kept for backward compatibility) =====

    @GetMapping("/by-users")
    public ApiResponse getBookingsByUserIds(@RequestParam List<Long> userIds) {
        return ApiResponse.builder()
                .status(200)
                .data(bookingService.getBookingsByUserIds(userIds))
                .build();
    }

    @GetMapping("/by-ids")
    public ApiResponse getBookingsByIdsMapped(@RequestParam List<Long> ids) {
        return ApiResponse.builder()
                .status(200)
                .data(bookingService.getBookingsByIdsMapped(ids))
                .build();
    }

    @GetMapping("/admin/guests")
    public ApiResponse getGuestBookingsForAdmin() {
        List<GuestBookingResponseDTO> data = bookingService.getGuestBookingsForAdmin();

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách đơn hàng của khách vãng lai thành công")
                .data(data)
                .build();
    }
}
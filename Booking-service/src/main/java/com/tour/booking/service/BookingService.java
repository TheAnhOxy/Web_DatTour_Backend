package com.tour.booking.service;

import com.tour.booking.dto.PassengerDTO;
import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.request.CancelBookingRequest;
import com.tour.booking.dto.response.*;

import java.util.List;
import java.util.Map;

public interface BookingService {
    
    // ===== GET OPERATIONS =====
    BookingDetailResponse getBookingByCode(String bookingCode);
    BookingDetailResponse getBookingById(Long bookingId);
    
    PaginatedResponse<BookingResponse> getBookingsByUserId(
            Long userId, 
            String status, 
            Integer page, 
            Integer limit);
    
    PaginatedResponse<BookingResponse> getAllBookings(
            String status, 
            String paymentMethod, 
            Integer page, 
            Integer limit);
    
    List<PassengerDTO> getPassengersByBookingCode(String bookingCode);
    List<BookingNoteDTO> getBookingNotesByBookingCode(String bookingCode);
    CancellationDTO getCancellationByBookingCode(String bookingCode);
    
    Map<Long, BookingDetailResponse> getBookingsByIds(List<Long> bookingIds);
    BookingSummaryDTO getUserBookingSummary(Long userId);
    
    // ===== EXISTING METHODS =====
    BookingResponse createBooking(BookingRequest request);
    void cancelBooking(CancelBookingRequest request);
    
    // ===== HELPER METHODS (optional) =====
    List<BookingResponse> getBookingsByUserIds(List<Long> userIds);
    Map<Long, BookingResponse> getBookingsByIdsMapped(List<Long> ids);
}

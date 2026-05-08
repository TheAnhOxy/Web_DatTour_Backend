package com.tour.booking.service;

import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.request.CancelBookingRequest;
import com.tour.booking.dto.response.BookingResponse;

import java.util.List;
import java.util.Map;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request);
    void cancelBooking(CancelBookingRequest request);
    BookingResponse getBookingByCode(String bookingCode);
    List<BookingResponse> getBookingsByUserId(Long userId);
    List<BookingResponse> getAllBookings();
    Map<Long, List<BookingResponse>> getBookingsByUserIds(List<Long> userIds);
    Map<Long, BookingResponse> getBookingsByIds(List<Long> ids);

}

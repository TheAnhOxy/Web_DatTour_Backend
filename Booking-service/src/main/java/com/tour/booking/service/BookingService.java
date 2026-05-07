package com.tour.booking.service;

import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.request.CancelBookingRequest;
import com.tour.booking.dto.response.BookingResponse;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request);
    void cancelBooking(CancelBookingRequest request);
    BookingResponse getBookingByCode(String bookingCode);

}

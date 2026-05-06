package com.tour.booking.service;

import com.tour.booking.dto.request.BookingRequest;
import com.tour.booking.dto.response.BookingResponse;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request);

}

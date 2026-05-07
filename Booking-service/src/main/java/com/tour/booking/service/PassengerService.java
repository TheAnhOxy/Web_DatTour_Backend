package com.tour.booking.service;

import com.tour.booking.dto.response.PassengerResponseDTO;
import com.tour.booking.entity.Passenger;

import java.util.List;
import java.util.Map;

public interface PassengerService {
    List<PassengerResponseDTO> getPassengerHistory(String idCardNumber);
    List<PassengerResponseDTO> getAllPassengers();
    PassengerResponseDTO getPassengerById(Long id);
}

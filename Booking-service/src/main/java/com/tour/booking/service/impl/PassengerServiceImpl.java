package com.tour.booking.service.impl;

import com.tour.booking.dto.response.PassengerResponseDTO;
import com.tour.booking.entity.Passenger;
import com.tour.booking.repository.PassengerRepository;
import com.tour.booking.service.PassengerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PassengerResponseDTO> getAllPassengers() {

        return passengerRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PassengerResponseDTO getPassengerById(Long id) {

        Passenger p = passengerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));

        return toDTO(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerResponseDTO> getPassengerHistory(String idCardNumber) {

        List<Passenger> records = passengerRepository.findByIdCardNumber(idCardNumber);

        return records.stream()
                .map(this::toDTO)
                .toList();
    }

    private PassengerResponseDTO toDTO(Passenger p) {

        var booking = p.getBooking();

        return PassengerResponseDTO.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .idCardNumber(p.getIdCardNumber())
                .gender(p.getGender())
                .ageGroup(p.getAgeGroup())

                .bookingCode(booking != null ? booking.getBookingCode() : null)
                .status(booking != null ? booking.getStatus() : null)
                .tourTitle(
                        booking != null && booking.getPriceSnapshot() != null
                                ? (String) booking.getPriceSnapshot().get("tourTitle")
                                : null
                )
                .totalAmount(booking != null ? booking.getTotalAmount() : null)
                .createdAt(booking != null ? booking.getCreatedAt() : null)

                .build();
    }
}
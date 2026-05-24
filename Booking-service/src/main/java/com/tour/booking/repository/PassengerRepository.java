package com.tour.booking.repository;

import com.tour.booking.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    
    @Query("SELECT p FROM Passenger p JOIN FETCH p.booking WHERE p.idCardNumber = :idCardNumber")
    List<Passenger> findByIdCardNumber(String idCardNumber);
    
    // Find all passengers by booking ID
    List<Passenger> findByBookingId(Long bookingId);
    
    // Find all passengers by booking code (via native query join with bookings table)
    @Query(value = "SELECT p.* FROM passenger p " +
                   "INNER JOIN bookings b ON p.booking_id = b.id " +
                   "WHERE b.booking_code = :bookingCode", nativeQuery = true)
    List<Passenger> findByBookingCode(@Param("bookingCode") String bookingCode);
}


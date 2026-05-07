package com.tour.booking.repository;

import com.tour.booking.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    @Query("SELECT p FROM Passenger p JOIN FETCH p.booking WHERE p.idCardNumber = :idCardNumber")
    List<Passenger> findByIdCardNumber(String idCardNumber);
}

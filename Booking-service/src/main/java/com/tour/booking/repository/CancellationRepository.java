package com.tour.booking.repository;

import com.tour.booking.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, Long> {
    
    // Find by booking ID (1:1 relationship)
    Optional<Cancellation> findByBookingId(Long bookingId);
    
    // Find by booking code
    @Query(value = "SELECT c.* FROM cancellations c " +
                   "JOIN bookings b ON c.booking_id = b.id " +
                   "WHERE b.booking_code = :bookingCode", 
           nativeQuery = true)
    Optional<Cancellation> findByBookingCode(@Param("bookingCode") String bookingCode);
}

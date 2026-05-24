package com.tour.booking.repository;

import com.tour.booking.entity.BookingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingNoteRepository extends JpaRepository<BookingNote, Long> {
    
    // Find all notes by booking, sorted by creation time
    List<BookingNote> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    
    // Find by booking code
    @Query(value = "SELECT bn.* FROM booking_notes bn " +
                   "JOIN bookings b ON bn.booking_id = b.id " +
                   "WHERE b.booking_code = :bookingCode " +
                   "ORDER BY bn.created_at DESC", 
           nativeQuery = true)
    List<BookingNote> findByBookingCode(@Param("bookingCode") String bookingCode);
}

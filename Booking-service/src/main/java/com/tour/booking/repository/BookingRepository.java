package com.tour.booking.repository;

import com.tour.booking.entity.Booking;
import com.tour.booking.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);

    /** Đơn online (chưa chọn thanh toán tại quầy) — hết hạn 10 phút */
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND (b.paymentMethod IS NULL OR b.paymentMethod <> 'CASH_OFFICE') AND b.createdAt < :cutoff")
    List<Booking> findOnlinePendingExpired(@Param("cutoff") LocalDateTime cutoff);

    /** Đơn thanh toán tại quầy — hết hạn theo paymentDueAt */
    List<Booking> findByStatusAndPaymentMethodAndPaymentDueAtBefore(
            String status, String paymentMethod, LocalDateTime paymentDueAt);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers")
    List<Booking> findAllWithPassengers();

    // Thêm câu lệnh này để lấy theo User kèm hành khách
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdWithPassengers(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers WHERE b.userId IN :userIds")
    List<Booking> findAllByUserIdsWithPassengers(@Param("userIds") List<Long> userIds);
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers WHERE b.id IN :ids")
    List<Booking> findAllByIdsWithPassengers(@Param("ids") List<Long> ids);
}
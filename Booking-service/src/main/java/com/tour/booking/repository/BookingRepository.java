package com.tour.booking.repository;

import com.tour.booking.entity.Booking;
import com.tour.booking.entity.Passenger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, 
                                          JpaSpecificationExecutor<Booking> {
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

    // ===== NEW GET API METHODS =====

    // 1. Find by Code with all relationships
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.bookingCode = :bookingCode")
    Optional<Booking> findByBookingCodeFetchAll(@Param("bookingCode") String bookingCode);

    // 2. Find by ID with all relationships
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.id = :id")
    Optional<Booking> findByIdFetchAll(@Param("id") Long id);

    // 3. Find by User ID with passengers (paginated)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.userId = :userId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdWithPassengersPaginated(
            @Param("userId") Long userId, 
            Pageable pageable);

    // 4. Find by User ID and Status
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.userId = :userId AND b.status = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdAndStatusWithPassengers(
            @Param("userId") Long userId, 
            @Param("status") String status, 
            Pageable pageable);

    // 5. Find all with passengers (paginated)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findAllWithPassengersPaginated(Pageable pageable);

    // 6. Find by status with passengers
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "WHERE b.status = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByStatusWithPassengers(
            @Param("status") String status, 
            Pageable pageable);

    // 7. Find by IDs with all relationships (batch)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.passengers " +
           "LEFT JOIN FETCH b.bookingNotes " +
           "LEFT JOIN FETCH b.cancellation " +
           "WHERE b.id IN :ids")
    List<Booking> findByIdInFetchAll(@Param("ids") List<Long> ids);

    // 8. Count by User ID
    Long countByUserId(Long userId);

    // 9. Count by User ID and Status
    Long countByUserIdAndStatus(Long userId, String status);

    // 10. Find by Payment Method and Due Date (Cleanup Task)
    @Query("SELECT b FROM Booking b " +
           "WHERE b.status = 'PENDING' " +
           "AND b.paymentMethod = :paymentMethod " +
           "AND b.paymentDueAt < :dueDate " +
           "ORDER BY b.createdAt ASC")
    List<Booking> findPendingByPaymentMethodAndDueDate(
            @Param("paymentMethod") String paymentMethod,
            @Param("dueDate") LocalDateTime dueDate,
            Pageable pageable);
}
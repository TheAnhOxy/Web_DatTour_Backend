package com.tour.payment.repository;

import com.tour.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByTransactionId(String transactionId);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoff AND p.transactionId IS NOT NULL")
    List<Payment> findPendingExpired(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.gateway = 'CASH_OFFICE' AND p.createdAt < :cutoff")
    List<Payment> findPendingOfficeExpired(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND (p.gateway IS NULL OR p.gateway <> 'CASH_OFFICE') AND p.createdAt < :cutoff AND p.transactionId IS NOT NULL")
    List<Payment> findPendingOnlineExpired(@Param("cutoff") LocalDateTime cutoff);
}
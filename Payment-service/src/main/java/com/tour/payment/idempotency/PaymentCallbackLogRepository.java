package com.tour.payment.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackLogRepository extends JpaRepository<PaymentCallbackLog, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}

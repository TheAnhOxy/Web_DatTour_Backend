package com.tour.payment.idempotency;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_callback_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_callback_idempotency", columnNames = "idempotency_key")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(nullable = false, length = 50)
    private String gateway;

    @Column(nullable = false, length = 120)
    private String transactionId;

    @Column(nullable = false, length = 30)
    private String status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

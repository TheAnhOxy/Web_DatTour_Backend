package com.tour.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId; // Tham chiếu sang Booking Service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    private BigDecimal amount;

    @Column(length = 120, unique = true) // UNIQUE để tránh trùng lặp giao dịch
    private String transactionId;

    private String gateway; // VNPAY, MOMO, STRIPE

    @Column(columnDefinition = "TEXT")
    private String paymentUrl;

    private String status; // PENDING, SUCCESS, FAILED

    private LocalDateTime paidAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
package com.tour.booking.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.redisson.api.JsonType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Entity
@Table(name = "bookings")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String bookingCode;

    @Column(name = "user_id", nullable = true)
    private Long userId; // ID từ Identity Service

    @Column(nullable = false)
    private Long departureId; // ID từ Core Service

    private BigDecimal totalAmount;
    private BigDecimal paidAmount;

    private String status; // PENDING, CONFIRMED, CANCELLED, COMPLETED

    /** BANK_TRANSFER, STRIPE, CASH_OFFICE — null = giữ chỗ online 10 phút */
    @Column(length = 30)
    private String paymentMethod;

    /** Hạn thanh toán tại quầy (thường createdAt + 24h) */
    private LocalDateTime paymentDueAt;

    private String contactName;
    private String contactEmail;
    private String contactPhone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> priceSnapshot;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingNote> bookingNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> promotionSnapshot;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<Passenger> passengers;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Cancellation cancellation;

    @Version // Optimistic Locking để chống race condition
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;


}
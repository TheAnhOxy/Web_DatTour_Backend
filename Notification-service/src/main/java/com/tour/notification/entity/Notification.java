package com.tour.notification.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // ID từ Identity Service

    private String templateCode; // WELCOME_EMAIL, BOOKING_SUCCESS, PAYMENT_CONFIRM

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String channel; // EMAIL, SMS, PUSH

    private String status; // PENDING, SENT, FAILED, READ

    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

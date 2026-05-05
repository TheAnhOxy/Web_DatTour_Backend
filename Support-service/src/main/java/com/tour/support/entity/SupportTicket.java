package com.tour.support.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "support_tickets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicket {
    @Id
    private String id;
    private Long userId;
    private String title;
    private String category; // REFUND, BOOKING, ACCOUNT
    private String content;
    private String priority; // LOW, MEDIUM, HIGH
    private String status;   // OPEN, IN_PROGRESS, CLOSED
    private Long assignedStaffId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
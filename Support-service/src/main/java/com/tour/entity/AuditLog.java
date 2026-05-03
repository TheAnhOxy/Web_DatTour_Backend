package com.tour.entity;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "audit_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    private String id;
    private String service; // booking-service, identity-service...
    private String entity;  // BOOKING, USER, TOUR
    private Long entityId;
    private String action;  // CREATED, UPDATED, DELETED, STATUS_CHANGED
    private Long actorId;
    private String actorName;
    private String actorRole;

    private Map<String, Object> oldData;
    private Map<String, Object> newData;

    private String ipAddress;
    private String userAgent;
    private String traceId; // ID để tracking luồng gọi giữa các service
    private LocalDateTime createdAt;
}

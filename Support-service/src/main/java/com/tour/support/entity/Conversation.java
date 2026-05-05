package com.tour.support.entity;



import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "conversations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {
    @Id
    private String id;
    private Long clientId;
    private Long staffId;
    private String status; // OPEN, CLOSED

    private List<Message> messages;

    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Message {
        private Long senderId;
        private String senderRole; // CLIENT, STAFF
        private String content;
        private Boolean seen;
        private LocalDateTime sentAt;
    }
}
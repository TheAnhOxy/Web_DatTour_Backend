package com.tour.support.entity;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "reviews")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id
    private String id;
    private Long tourId;
    private Long userId;
    private Integer rating;
    private String comment;

    private List<Media> media;
    private List<Reply> replies;

    private String status; // VISIBLE, HIDDEN, REPORTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @AllArgsConstructor
    public static class Media {
        private String url;
        private String type; // IMAGE, VIDEO
    }

    @Data @AllArgsConstructor
    public static class Reply {
        private Long staffId;
        private String content;
        private LocalDateTime createdAt;
    }
}
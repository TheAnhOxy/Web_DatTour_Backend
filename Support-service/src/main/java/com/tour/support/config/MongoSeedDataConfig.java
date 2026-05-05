package com.tour.support.config;

import com.tour.support.entity.AuditLog;
import com.tour.support.entity.Conversation;
import com.tour.support.entity.Review;
import com.tour.support.entity.SupportTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoSeedDataConfig {

    private final MongoTemplate mongoTemplate;

    @Bean
    CommandLineRunner runner() {
        return args -> {
            seedReviews();
            seedSupportTickets();
            seedConversations();
            seedAuditLogs();
        };
    }

    private void seedReviews() {
        if (mongoTemplate.findAll(Review.class).isEmpty()) {
            Review review = Review.builder()
                    .tourId(1L)
                    .userId(1L)
                    .rating(5)
                    .comment("Tour Phú Quốc quá tuyệt vời, hướng dẫn viên nhiệt tình!")
                    .media(List.of(new Review.Media("https://img.com/phuquoc.jpg", "IMAGE")))
                    .replies(List.of(new Review.Reply(88L, "Cảm ơn Thế Anh đã đánh giá!", LocalDateTime.now())))
                    .status("VISIBLE")
                    .createdAt(LocalDateTime.now())
                    .build();
            mongoTemplate.save(review);
            log.info("✓ Seeded Reviews collection");
        }
    }

    private void seedSupportTickets() {
        if (mongoTemplate.findAll(SupportTicket.class).isEmpty()) {
            SupportTicket ticket = SupportTicket.builder()
                    .userId(1L)
                    .title("Yêu cầu hoàn tiền")
                    .category("REFUND")
                    .content("Tôi muốn hủy tour do bận việc đột xuất")
                    .priority("HIGH")
                    .status("OPEN")
                    .assignedStaffId(99L)
                    .createdAt(LocalDateTime.now())
                    .build();
            mongoTemplate.save(ticket);
            log.info("✓ Seeded SupportTickets collection");
        }
    }

    private void seedConversations() {
        if (mongoTemplate.findAll(Conversation.class).isEmpty()) {
            Conversation chat = Conversation.builder()
                    .clientId(1L)
                    .staffId(88L)
                    .status("OPEN")
                    .messages(List.of(
                            new Conversation.Message(1L, "CLIENT", "Xin chào, hỗ trợ mình với", true, LocalDateTime.now()),
                            new Conversation.Message(88L, "STAFF", "Chào Thế Anh, mình có thể giúp gì cho bạn?", false, LocalDateTime.now())
                    ))
                    .lastMessageAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            mongoTemplate.save(chat);
            log.info("✓ Seeded Conversations collection");
        }
    }

    private void seedAuditLogs() {
        if (mongoTemplate.findAll(AuditLog.class).isEmpty()) {
            AuditLog logEntry = AuditLog.builder()
                    .service("booking-service")
                    .entity("BOOKING")
                    .entityId(1023L)
                    .action("STATUS_CHANGED")
                    .actorId(1L)
                    .actorName("Nguyễn Thế Anh")
                    .actorRole("ADMIN")
                    .oldData(Map.of("status", "PENDING"))
                    .newData(Map.of("status", "CONFIRMED"))
                    .ipAddress("127.0.0.1")
                    .traceId("trace-booking-001")
                    .createdAt(LocalDateTime.now())
                    .build();
            mongoTemplate.save(logEntry);
            log.info("✓ Seeded AuditLogs collection");
        }
    }
}
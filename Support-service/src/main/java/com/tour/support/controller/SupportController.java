package com.tour.support.controller;

import com.tour.support.dto.response.ApiResponse;
import com.tour.support.entity.Review;
import com.tour.support.entity.SupportTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/supports")
@RequiredArgsConstructor
public class SupportController {

    private final MongoTemplate mongoTemplate;

    @GetMapping("/reviews")
    public List<Review> getReviews() {
        return mongoTemplate.findAll(Review.class);
    }

    @PostMapping("/tickets")
    public ApiResponse createTicket(@RequestBody SupportTicket ticket) {
        if (ticket.getCreatedAt() == null) {
            ticket.setCreatedAt(LocalDateTime.now());
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        if (ticket.getStatus() == null) {
            ticket.setStatus("OPEN");
        }
        SupportTicket savedTicket = mongoTemplate.save(ticket);
        return ApiResponse.builder()
                .status(200)
                .message("Tạo ticket hỗ trợ thành công")
                .data(savedTicket)
                .build();
    }

    @GetMapping
    public String getHello(){
        return "hello sup";
    }
}
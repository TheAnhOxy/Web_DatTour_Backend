package com.tour.support.controller;


import com.tour.support.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public String getHello(){
        return "hello sup";
    }

//    @PostMapping
//    public ResponseEntity<ApiResponse> create(@RequestBody FoodRequest request) {
//        return ResponseEntity.status(201).body(ApiResponse.builder()
//                .status(201)
//                .message("Thêm món thành công")
//                .data(foodService.createFood(request))
//                .build());
//    }

}